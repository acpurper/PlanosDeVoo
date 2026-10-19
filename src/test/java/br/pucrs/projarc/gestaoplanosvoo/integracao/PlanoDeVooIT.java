package br.pucrs.projarc.gestaoplanosvoo.integracao;

import br.pucrs.projarc.gestaoplanosvoo.domain.model.Altitude;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.SlotHorario;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Vaga;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.reserva.Reserva;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.reserva.ReservaAerovia;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.reserva.ReservaVaga;
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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SuppressWarnings("unused") // campos e beans são usados pelo JUnit, Testcontainers e Spring
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.relogio-fixo=2026-11-01T00:00")
class PlanoDeVooIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    static volatile CyclicBarrier barreira;

    /** Tempo que a aprovação segura o lock do plano depois da barreira, para outra requisição chegar nesse meio. */
    static volatile long pausaAposBarreiraMs;

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
                            Thread.sleep(pausaAposBarreiraMs);
                        } catch (InterruptedException | BrokenBarrierException | TimeoutException e) {
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
                public List<ReservaAerovia> listarDaAerovia(String aeroviaId, LocalDateTime inicio, LocalDateTime fim) {
                    return real.listarDaAerovia(aeroviaId, inicio, fim);
                }

                @Override
                public List<ReservaVaga> listarDoAeroporto(String aeroportoCodigo, LocalDateTime inicio, LocalDateTime fim) {
                    return real.listarDoAeroporto(aeroportoCodigo, inicio, fim);
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
        pausaAposBarreiraMs = 0;
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
        assertThat(corpo(resposta).get("violacoes").toString()).contains("ALTITUDE_OCUPADA");
        assertThat(reservasDoPlano(id)).isZero();
        assertThat(totalReservas()).isEqualTo(antes);
        assertThat(situacao(id)).isEqualTo("EM_EDICAO");
    }

    @Test
    void conflitoEntreTrechosDoProprioPlanoDa422ENao409() {
        long id = criarPlano("PR-AZA", "POA", "CWB", "2026-12-06T09:00");
        adicionarTrecho(id, "R1", 30_000, "2026-12-06T09:00", null, null);
        adicionarTrecho(id, "R8", 30_000, "2026-12-06T09:26", null, null);
        adicionarTrecho(id, "R1", 30_000, "2026-12-06T09:52", null, null);
        adicionarTrecho(id, "R4", 30_000, "2026-12-06T10:18", 1, 30);
        int antes = totalReservas();

        ResponseEntity<Map<String, Object>> validacao = post("/planos/" + id + "/validacao");
        assertThat(validacao.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(validacao.getBody()).containsEntry("valido", false);
        assertThat(corpo(validacao).get("violacoes").toString())
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
    void edicaoDuranteAAprovacaoEsperaOLockERecebe409() throws Exception {
        long id = criarPlano("PR-AZA", "CWB", "GRU", "2026-12-05T20:00");
        adicionarTrecho(id, "R6", 34_000, "2026-12-05T20:00", 30, 30);

        // a aprovação para depois de validar, segurando o lock do plano; a edição chega nesse meio
        barreira = new CyclicBarrier(2);
        pausaAposBarreiraMs = 1_500;
        CompletableFuture<ResponseEntity<Map<String, Object>>> aprovacao =
                CompletableFuture.supplyAsync(() -> post("/planos/" + id + "/aprovacao"));
        barreira.await(20, TimeUnit.SECONDS);
        // incluir trecho só insere linhas em "trecho" e não incrementa a versão do plano
        ResponseEntity<Map<String, Object>> edicao = http.exchange("/planos/" + id + "/trechos", HttpMethod.POST,
                new HttpEntity<>(trecho("R7", 35_000, "2026-12-05T21:00", 3, 30)), MAPA);

        assertThat(aprovacao.get(30, TimeUnit.SECONDS).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(edicao.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(edicao.getBody()).containsEntry("codigo", "PLANO_NAO_EDITAVEL");
        assertThat(jdbc.queryForObject("select count(*) from trecho where plano_id = ?", Integer.class, id))
                .as("o percurso aprovado não pode mudar depois das reservas").isEqualTo(1);
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
        assertThat(((List<?>) corpo(resposta).get("trechos"))).as("dados do plano preservados").hasSize(1);

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

    @Test
    void consultarPlanoInicialDevolveTrechosHorariosEReservas() {
        ResponseEntity<Map<String, Object>> r = http.exchange("/planos/1", HttpMethod.GET, null, MAPA);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).containsEntry("situacao", "APROVADO");
        assertThat((List<Object>) corpo(r).get("trechos")).hasSize(1);
        assertThat((List<Object>) corpo(r).get("reservas")).isNotEmpty();
    }

    @Test
    void planoInexistenteResponde404EmTodasAsOperacoes() {
        assertThat(http.exchange("/planos/999999", HttpMethod.GET, null, MAPA).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(post("/planos/999999/validacao").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(post("/planos/999999/aprovacao").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(post("/planos/999999/cancelamento").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void criarPlanoComCamposAusentesResponde400ListandoOsCampos() {
        ResponseEntity<Map<String, Object>> r = http.exchange("/planos", HttpMethod.POST,
                new HttpEntity<>(Map.of("aeronave", "PR-AZA")), MAPA);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getBody()).containsEntry("codigo", "REQUISICAO_INVALIDA");
        assertThat(corpo(r).get("campos").toString()).contains("pilotoId", "origem", "destino", "dataHoraPartida");
    }

    @Test
    void criarPlanoComOrigemIgualAoDestinoResponde422() {
        Map<String, Object> corpo = Map.of("pilotoId", 1, "aeronave", "PR-AZA", "origem", "POA",
                "destino", "POA", "dataHoraPartida", "2026-12-07T09:00");

        ResponseEntity<Map<String, Object>> r = http.exchange("/planos", HttpMethod.POST, new HttpEntity<>(corpo), MAPA);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(r.getBody()).containsEntry("codigo", "ORIGEM_IGUAL_DESTINO");
    }

    @Test
    void criarPlanoComPilotoAeronaveOuAeroportoInexistenteResponde404() {
        Map<String, Object> base = new HashMap<>(Map.of("pilotoId", 1, "aeronave", "PR-AZA", "origem", "POA",
                "destino", "FLN", "dataHoraPartida", "2026-12-07T09:00"));
        for (Map.Entry<String, Object> invalido : Map.<String, Object>of("pilotoId", 999, "aeronave", "XX-000",
                "destino", "ZZZ").entrySet()) {
            Map<String, Object> corpo = new HashMap<>(base);
            corpo.put(invalido.getKey(), invalido.getValue());
            assertThat(http.exchange("/planos", HttpMethod.POST, new HttpEntity<>(corpo), MAPA).getStatusCode())
                    .as(invalido.getKey()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Test
    void trechoComAltitudeInvalidaAeroviaOuVagaInexistenteRespondeComErroDeCliente() {
        long id = criarPlano("PR-AZA", "CWB", "GRU", "2026-12-07T09:00");

        assertThat(http.exchange("/planos/" + id + "/trechos", HttpMethod.POST,
                new HttpEntity<>(trecho("R6", 24_000, "2026-12-07T09:00", 1, 30)), MAPA).getStatusCode())
                .as("altitude fora de 25.000 a 35.000").isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(http.exchange("/planos/" + id + "/trechos", HttpMethod.POST,
                new HttpEntity<>(trecho("R99", 30_000, "2026-12-07T09:00", 1, 30)), MAPA).getStatusCode())
                .as("aerovia inexistente").isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(http.exchange("/planos/" + id + "/trechos", HttpMethod.POST,
                new HttpEntity<>(trecho("R6", 30_000, "2026-12-07T09:00", 500, 30)), MAPA).getStatusCode())
                .as("vaga inexistente em GRU").isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(http.exchange("/planos/" + id + "/trechos/9", HttpMethod.DELETE, null, MAPA).getStatusCode())
                .as("trecho inexistente").isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void cancelarPlanoEmEdicaoResponde409() {
        long id = criarPlano("PR-AZA", "CWB", "GRU", "2026-12-08T09:00");

        ResponseEntity<Map<String, Object>> r = post("/planos/" + id + "/cancelamento");

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(r.getBody()).containsEntry("codigo", "PLANO_NAO_CANCELAVEL");
    }

    @Test
    void abandonarPlanoEmEdicaoOMantemConsultavelESemAlteracoes() {
        long id = criarPlano("PR-AZA", "CWB", "GRU", "2026-12-09T10:00");
        adicionarTrecho(id, "R6", 30_000, "2026-12-09T10:00", 10, 30);

        ResponseEntity<Map<String, Object>> r = post("/planos/" + id + "/abandono");

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(corpo(r)).containsEntry("situacao", "ABANDONADO");
        assertThat(reservasDoPlano(id)).isZero();
        assertThat(get("/planos/" + id).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(post("/planos/" + id + "/validacao").getBody()).containsEntry("codigo", "PLANO_NAO_EDITAVEL");
        assertThat(post("/planos/" + id + "/aprovacao").getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(post("/planos/" + id + "/abandono").getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void abandonarPlanoAprovadoResponde409() {
        ResponseEntity<Map<String, Object>> r = post("/planos/2/abandono");

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(r.getBody()).containsEntry("codigo", "PLANO_NAO_EDITAVEL");
        assertThat(situacao(2)).isEqualTo("APROVADO");
    }

    @Test
    void listarPorSituacaoDevolveSoPlanosDaquelaSituacao() {
        for (String s : List.of("EM_EDICAO", "APROVADO", "CANCELADO", "ABANDONADO")) {
            ResponseEntity<List<Map<String, Object>>> r = http.exchange("/planos?situacao=" + s, HttpMethod.GET, null, LISTA);
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(r.getBody()).as("planos %s da carga inicial", s).isNotEmpty()
                    .allSatisfy(p -> assertThat(p).containsEntry("situacao", s));
        }
        ResponseEntity<List<Map<String, Object>>> todos = http.exchange("/planos", HttpMethod.GET, null, LISTA);
        assertThat(todos.getBody()).hasSizeGreaterThanOrEqualTo(9);
    }

    @Test
    void listarComSituacaoDesconhecidaResponde400() {
        ResponseEntity<Map<String, Object>> r = get("/planos?situacao=VOANDO");

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getBody()).containsEntry("codigo", "REQUISICAO_INVALIDA");
    }

    @SuppressWarnings("unchecked")
    @Test
    void altitudesLivresRefletemAsReservasDaCargaInicial() {
        // Plano 1: R2 a 30.000 nos slots 08h e 09h
        ResponseEntity<Map<String, Object>> r = get("/aerovias/R2/altitudes-livres?inicio=2026-12-01T08:30&fim=2026-12-01T09:30");

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((List<Object>) corpo(r).get("altitudesDisponiveis")).hasSize(10).doesNotContain(30_000);
        assertThat((List<Object>) corpo(r).get("altitudesOcupadas")).hasSize(1);
    }

    @SuppressWarnings("unchecked")
    @Test
    void vagasLivresRefletemAsReservasDaCargaInicial() {
        // Plano 1: GRU-1 nos slots 09h e 10h
        ResponseEntity<Map<String, Object>> r = get("/aeroportos/GRU/vagas-livres?inicio=2026-12-01T10:00&fim=2026-12-01T10:30");

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((List<Object>) corpo(r).get("vagasDisponiveis")).doesNotContain(1).contains(2);
        assertThat(corpo(r)).containsEntry("totalVagas", 115);
    }

    @Test
    void consultaDeDisponibilidadeComPeriodoInvalidoOuRecursoInexistente() {
        assertThat(get("/aerovias/R2/altitudes-livres?inicio=2026-12-01T10:00&fim=2026-12-01T09:00").getBody())
                .containsEntry("codigo", "PERIODO_INVALIDO");
        assertThat(get("/aerovias/R99/altitudes-livres?inicio=2026-12-01T08:00&fim=2026-12-01T09:00").getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(get("/aeroportos/GRU/vagas-livres?inicio=2026-12-01T08:00").getBody())
                .containsEntry("codigo", "REQUISICAO_INVALIDA");
        assertThat(get("/aeroportos/GRU/vagas-livres?inicio=ontem&fim=hoje").getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void consultasDaMalhaEDosRecursos() {
        assertThat(http.exchange("/aeroportos", HttpMethod.GET, null, LISTA).getBody()).hasSizeGreaterThanOrEqualTo(4);
        assertThat(http.exchange("/aerovias", HttpMethod.GET, null, LISTA).getBody()).hasSizeGreaterThanOrEqualTo(8);
        assertThat(http.exchange("/pilotos", HttpMethod.GET, null, LISTA).getBody()).hasSizeGreaterThanOrEqualTo(4);
        assertThat(http.exchange("/aeronaves", HttpMethod.GET, null, LISTA).getBody())
                .extracting(a -> a.get("categoria")).contains("PARTICULAR", "PASSAGEIROS", "CARGA");
        assertThat(get("/aerovias/R1").getBody()).containsEntry("origem", "POA").containsEntry("destino", "FLN");
        assertThat(get("/aeronaves/PR-CGB").getBody()).containsEntry("capacidadeToneladas", 12.0);
        assertThat(get("/aeroportos/XXX").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private static Map<String, Object> corpo(ResponseEntity<Map<String, Object>> resposta) {
        return Objects.requireNonNull(resposta.getBody(), "resposta sem corpo");
    }

    private static final ParameterizedTypeReference<Map<String, Object>> MAPA =new ParameterizedTypeReference<>() {
    };

    private long criarPlano(String aeronave, String origem, String destino, String partida) {
        Map<String, Object> corpo = Map.of("pilotoId", 1, "aeronave", aeronave, "origem", origem,
                "destino", destino, "dataHoraPartida", partida);
        ResponseEntity<Map<String, Object>> r = http.exchange("/planos", HttpMethod.POST, new HttpEntity<>(corpo), MAPA);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return ((Number) corpo(r).get("id")).longValue();
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

    private static final ParameterizedTypeReference<List<Map<String, Object>>> LISTA = new ParameterizedTypeReference<>() {
    };

    private ResponseEntity<Map<String, Object>> get(String caminho) {
        return http.exchange(caminho, HttpMethod.GET, null, MAPA);
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
