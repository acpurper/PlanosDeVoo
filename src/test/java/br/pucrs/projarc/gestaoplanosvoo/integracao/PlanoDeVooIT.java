package br.pucrs.projarc.gestaoplanosvoo.integracao;

import br.pucrs.projarc.gestaoplanosvoo.domain.model.Altitude;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.SlotHorario;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Vaga;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.reserva.Reserva;
import br.pucrs.projarc.gestaoplanosvoo.domain.repository.ReservaRepositoryPort;
import br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.adapters.ReservaRepositoryAdapter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de integração contra um PostgreSQL real (Testcontainers), passando pela API REST.
 * O "agora" é fixado em 2026-11-01 para que a data de referência (2026-12-01) seja sempre futura.
 * Cada teste usa um dia próprio para não depender dos outros nem da carga inicial.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.relogio-fixo=2026-11-01T00:00")
class PlanoDeVooIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    /** Quando definida, faz as aprovações esperarem umas pelas outras logo antes de gravar as reservas. */
    static volatile CyclicBarrier barreira;

    @TestConfiguration
    static class SincronizacaoDeAprovacoes {

        @Bean
        @Primary
        ReservaRepositoryPort reservasSincronizadas(ReservaRepositoryAdapter real) {
            return new ReservaRepositoryPort() {
                @Override
                public void salvarTodas(List<Reserva> reservas) {
                    CyclicBarrier b = barreira;
                    if (b != null) {
                        try {
                            b.await(20, TimeUnit.SECONDS);
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        }
                    }
                    real.salvarTodas(reservas);
                }

                @Override
                public void removerDoPlano(Long planoId) {
                    real.removerDoPlano(planoId);
                }

                @Override
                public List<Reserva> listarDoPlano(Long planoId) {
                    return real.listarDoPlano(planoId);
                }

                @Override
                public boolean altitudeOcupada(String aeroviaId, Altitude altitude, SlotHorario slot) {
                    return real.altitudeOcupada(aeroviaId, altitude, slot);
                }

                @Override
                public boolean vagaOcupada(Vaga vaga, SlotHorario slot) {
                    return real.vagaOcupada(vaga, slot);
                }
            };
        }
    }

    @Autowired
    TestRestTemplate http;

    @Autowired
    JdbcTemplate jdbc;

    @AfterEach
    void limparBarreira() {
        barreira = null;
    }

    @Test
    void cargaInicialOcupaPeloMenosCincoAeroviasNaDataDeReferencia() {
        Integer aerovias = jdbc.queryForObject(
                "select count(distinct aerovia_id) from reserva_aerovia where slot_inicio::date = date '2026-12-01'",
                Integer.class);
        assertThat(aerovias).isGreaterThanOrEqualTo(5);
    }

    @Test
    void validarNaoCriaReservasNemMudaASituacao() {
        // R6 CWB->GRU, 360 km a 850 km/h = 26 min
        long id = criarPlano("PR-AZA", "CWB", "GRU", "2026-12-02T10:00");
        adicionarTrecho(id, "R6", 30_000, "2026-12-02T10:00", 10, 30);
        int antes = totalReservas();

        ResponseEntity<Map<String, Object>> resposta = post("/planos/" + id + "/validacao");

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).containsEntry("valido", true);
        assertThat(totalReservas()).isEqualTo(antes);
        assertThat(situacao(id)).isEqualTo("EM_EDICAO");
    }

    @Test
    void aprovacaoComViolacaoNaoGravaNadaEPlanoContinuaEmEdicao() {
        // trecho 1 livre (R1 a 26.000 às 13h); trecho 2 em conflito com o plano inicial 2 (R4 a 26.000 às 14h)
        long id = criarPlano("PP-JAT", "POA", "CWB", "2026-12-01T13:00");
        adicionarTrecho(id, "R1", 26_000, "2026-12-01T13:00", 3, 30);   // chega 13:37, sai 14:07
        adicionarTrecho(id, "R4", 26_000, "2026-12-01T14:07", 5, 30);   // 14:07 -> 14:32
        int antes = totalReservas();

        ResponseEntity<Map<String, Object>> resposta = post("/planos/" + id + "/aprovacao");

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(resposta.getBody().get("violacoes").toString()).contains("ALTITUDE_OCUPADA");
        assertThat(reservasDoPlano(id)).isZero();
        assertThat(totalReservas()).isEqualTo(antes);
        assertThat(situacao(id)).isEqualTo("EM_EDICAO");
    }

    @Test
    void conflitoEntreTrechosDoProprioPlanoDa422ENao409() {
        // PR-AZA a 30.000 pés: R1 09:00→09:26, R8 09:26→09:52, R1 09:52→10:18, R4 10:18→10:36.
        // Os dois usos de R1 caem no slot das 09h; antes da correção a UNIQUE falhava na gravação (409 falso).
        long id = criarPlano("PR-AZA", "POA", "CWB", "2026-12-06T09:00");
        adicionarTrecho(id, "R1", 30_000, "2026-12-06T09:00", null, null);
        adicionarTrecho(id, "R8", 30_000, "2026-12-06T09:26", null, null);
        adicionarTrecho(id, "R1", 30_000, "2026-12-06T09:52", null, null);
        adicionarTrecho(id, "R4", 30_000, "2026-12-06T10:18", 1, 30);
        int antes = totalReservas();

        ResponseEntity<Map<String, Object>> validacao = post("/planos/" + id + "/validacao");
        assertThat(validacao.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(validacao.getBody()).containsEntry("valido", false);
        assertThat(validacao.getBody().get("violacoes").toString())
                .contains("ALTITUDE_OCUPADA", "já usada pelo trecho 1 deste plano");

        ResponseEntity<Map<String, Object>> aprovacao = post("/planos/" + id + "/aprovacao");
        assertThat(aprovacao.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(aprovacao.getBody()).containsEntry("codigo", "PLANO_INVALIDO");
        assertThat(reservasDoPlano(id)).isZero();
        assertThat(totalReservas()).isEqualTo(antes);
        assertThat(situacao(id)).isEqualTo("EM_EDICAO");
    }

    @Test
    void aprovacoesConcorrentesDoMesmoSlotResultamEmUmaAprovadaEUmaConflito() throws Exception {
        // dois planos válidos que disputam R6 a 33.000 no slot das 20h; vagas diferentes em GRU
        long a = criarPlano("PR-AZA", "CWB", "GRU", "2026-12-03T20:00");
        adicionarTrecho(a, "R6", 33_000, "2026-12-03T20:00", 20, 30);
        long b = criarPlano("PR-GOB", "CWB", "GRU", "2026-12-03T20:10");
        adicionarTrecho(b, "R6", 33_000, "2026-12-03T20:10", 21, 30);

        // as duas validam com o recurso livre e só então tentam gravar ao mesmo tempo
        barreira = new CyclicBarrier(2);
        CompletableFuture<ResponseEntity<Map<String, Object>>> ra = CompletableFuture.supplyAsync(() -> post("/planos/" + a + "/aprovacao"));
        CompletableFuture<ResponseEntity<Map<String, Object>>> rb = CompletableFuture.supplyAsync(() -> post("/planos/" + b + "/aprovacao"));
        HttpStatus sa = HttpStatus.valueOf(ra.get(30, TimeUnit.SECONDS).getStatusCode().value());
        HttpStatus sb = HttpStatus.valueOf(rb.get(30, TimeUnit.SECONDS).getStatusCode().value());

        assertThat(List.of(sa, sb)).containsExactlyInAnyOrder(HttpStatus.OK, HttpStatus.CONFLICT);
        long vencedor = sa == HttpStatus.OK ? a : b;
        long perdedor = sa == HttpStatus.OK ? b : a;
        assertThat(situacao(vencedor)).isEqualTo("APROVADO");
        assertThat(situacao(perdedor)).isEqualTo("EM_EDICAO");
        assertThat(reservasDoPlano(perdedor)).as("rollback total: nenhuma reserva parcial").isZero();
        assertThat(reservasDoPlano(vencedor)).isPositive();
    }

    @Test
    void cancelarPlanoFuturoLiberaAsReservas() {
        long id = criarPlano("PP-JAT", "CWB", "GRU", "2026-12-04T09:00");
        adicionarTrecho(id, "R6", 25_000, "2026-12-04T09:00", 7, 60);
        assertThat(post("/planos/" + id + "/aprovacao").getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reservasDoPlano(id)).isPositive();

        ResponseEntity<Map<String, Object>> resposta = post("/planos/" + id + "/cancelamento");

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).containsEntry("situacao", "CANCELADO");
        assertThat(reservasDoPlano(id)).isZero();
        assertThat(((List<?>) resposta.getBody().get("trechos"))).as("dados do plano preservados").hasSize(1);

        // o recurso liberado pode ser aprovado por outro plano
        long outro = criarPlano("PP-JAT", "CWB", "GRU", "2026-12-04T09:00");
        adicionarTrecho(outro, "R6", 25_000, "2026-12-04T09:00", 7, 60);
        assertThat(post("/planos/" + outro + "/aprovacao").getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void cancelarPlanoComPartidaJaOcorridaResponde409() {
        long id = criarPlano("PP-JAT", "CWB", "GRU", "2026-10-01T09:00");
        adicionarTrecho(id, "R6", 25_000, "2026-10-01T09:00", 7, 60);
        assertThat(post("/planos/" + id + "/aprovacao").getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map<String, Object>> resposta = post("/planos/" + id + "/cancelamento");

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody()).containsEntry("codigo", "VOO_JA_EXECUTADO");
        assertThat(situacao(id)).isEqualTo("APROVADO");
        assertThat(reservasDoPlano(id)).isPositive();
    }

    @Test
    void editarPlanoAprovadoResponde409() {
        long id = criarPlano("PR-AZA", "CWB", "GRU", "2026-12-05T11:00");
        adicionarTrecho(id, "R6", 30_000, "2026-12-05T11:00", 3, 30);
        assertThat(post("/planos/" + id + "/aprovacao").getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, Object> trecho = trecho("R6", 31_000, "2026-12-05T11:00", 3, 30);
        assertThat(http.exchange("/planos/" + id + "/trechos", HttpMethod.POST, new HttpEntity<>(trecho), MAPA)
                .getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(http.exchange("/planos/" + id + "/trechos/1", HttpMethod.PUT, new HttpEntity<>(trecho), MAPA)
                .getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(http.exchange("/planos/" + id + "/trechos/1", HttpMethod.DELETE, null, MAPA)
                .getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(post("/planos/" + id + "/validacao").getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    // ------------------------------------------------------------------ apoio

    private static final ParameterizedTypeReference<Map<String, Object>> MAPA = new ParameterizedTypeReference<>() {
    };

    private long criarPlano(String aeronave, String origem, String destino, String partida) {
        Map<String, Object> corpo = Map.of("pilotoId", 1, "aeronave", aeronave, "origem", origem,
                "destino", destino, "dataHoraPartida", partida);
        ResponseEntity<Map<String, Object>> r = http.exchange("/planos", HttpMethod.POST, new HttpEntity<>(corpo), MAPA);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return ((Number) r.getBody().get("id")).longValue();
    }

    private void adicionarTrecho(long plano, String aerovia, int altitude, String entrada, Integer vaga, Integer permanencia) {
        ResponseEntity<Map<String, Object>> r = http.exchange("/planos/" + plano + "/trechos", HttpMethod.POST,
                new HttpEntity<>(trecho(aerovia, altitude, entrada, vaga, permanencia)), MAPA);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private static Map<String, Object> trecho(String aerovia, int altitude, String entrada, Integer vaga, Integer permanencia) {
        Map<String, Object> t = new HashMap<>();
        t.put("aerovia", aerovia);
        t.put("altitudePes", altitude);
        t.put("horarioEntrada", entrada);
        if (vaga != null) {
            t.put("pouso", Map.of("vaga", vaga, "permanenciaMin", permanencia));
        }
        return t;
    }

    private ResponseEntity<Map<String, Object>> post(String caminho) {
        return http.exchange(caminho, HttpMethod.POST, null, MAPA);
    }

    private String situacao(long plano) {
        return jdbc.queryForObject("select situacao from plano_voo where id = ?", String.class, plano);
    }

    private int reservasDoPlano(long plano) {
        return jdbc.queryForObject("select (select count(*) from reserva_aerovia where plano_id = ?)"
                + " + (select count(*) from reserva_vaga where plano_id = ?)", Integer.class, plano, plano);
    }

    private int totalReservas() {
        return jdbc.queryForObject("select (select count(*) from reserva_aerovia) + (select count(*) from reserva_vaga)",
                Integer.class);
    }
}
