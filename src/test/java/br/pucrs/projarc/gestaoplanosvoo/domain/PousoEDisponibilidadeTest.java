package br.pucrs.projarc.gestaoplanosvoo.domain;

import br.pucrs.projarc.gestaoplanosvoo.domain.exception.PlanoInvalidoException;
import br.pucrs.projarc.gestaoplanosvoo.domain.exception.RegraDeNegocioException;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.CodigoViolacao;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.PlanoDeVoo;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Pouso;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.ResultadoValidacao;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.SituacaoPlano;
import org.junit.jupiter.api.Test;

import static br.pucrs.projarc.gestaoplanosvoo.domain.Malha.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Regras 8 e 9: vaga do aeroporto certo e recursos livres em todos os slots. */
class PousoEDisponibilidadeTest {

    @Test
    void vagaPrecisaPertencerAoDestinoDaAerovia() {
        PlanoDeVoo plano = plano(PARTICULAR, POA, FLN, as(14, 0));
        plano.adicionarTrecho(R1, alt(26_000), as(14, 0), new Pouso(vaga(CWB, 1), 30));
        assertThat(plano.validar(TUDO_LIVRE).violacoes())
                .extracting(v -> v.codigo()).containsExactly(CodigoViolacao.VAGA_FORA_DO_AEROPORTO);
    }

    @Test
    void permanenciaPrecisaSerPositiva() {
        assertThatThrownBy(() -> new Pouso(vaga(FLN, 1), 0)).isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void altitudeLivreEmTodosOsSlotsPassa() {
        PlanoDeVoo plano = planoR2(as(8, 10));
        Ocupacao ocupacao = new Ocupacao().ocuparAltitude(R2, 31_000, as(8, 0)); // outra altitude
        assertThat(plano.validar(ocupacao).valido()).isTrue();
    }

    @Test
    void altitudeOcupadaEmUmDosSlotsFalha() {
        // 08:10 -> 09:12 ocupa 08h e 09h; reserva existente só no slot das 09h
        PlanoDeVoo plano = planoR2(as(8, 10));
        Ocupacao ocupacao = new Ocupacao().ocuparAltitude(R2, 30_000, as(9, 0));
        assertThat(plano.validar(ocupacao).violacoes()).singleElement()
                .satisfies(v -> {
                    assertThat(v.codigo()).isEqualTo(CodigoViolacao.ALTITUDE_OCUPADA);
                    assertThat(v.mensagem()).contains("09:00");
                });
    }

    @Test
    void vagaOcupadaDuranteAPermanenciaFalha() {
        // chegada 09:12 + 60 min ocupa a vaga GRU-1 às 09h e às 10h
        PlanoDeVoo plano = planoR2(as(8, 10));
        Ocupacao ocupacao = new Ocupacao().ocuparVaga(vaga(GRU, 1), as(10, 0));
        assertThat(plano.validar(ocupacao).contem(CodigoViolacao.VAGA_OCUPADA)).isTrue();

        Ocupacao outraVaga = new Ocupacao().ocuparVaga(vaga(GRU, 2), as(10, 0));
        assertThat(plano.validar(outraVaga).valido()).isTrue();
    }

    private PlanoDeVoo planoR2(java.time.LocalDateTime partida) {
        PlanoDeVoo plano = plano(PASSAGEIROS, POA, GRU, partida);
        plano.adicionarTrecho(R2, alt(30_000), partida, new Pouso(vaga(GRU, 1), 60));
        return plano;
    }

    /**
     * PR-AZA (850 km/h), tudo a 30.000 pés: R1 = 26 min, R8 = 26 min, R4 = 18 min.
     * R1 09:00→09:26, R8 09:26→09:52, R1 09:52→10:18, R4 10:18→10:36 com pouso CWB-1.
     * Os dois usos de R1 caem no slot das 09h.
     */
    private PlanoDeVoo planoQueRepeteR1(int altitudeDoSegundoR1) {
        PlanoDeVoo plano = plano(PASSAGEIROS, POA, CWB, as(9, 0));
        plano.adicionarTrecho(R1, alt(30_000), as(9, 0), null);
        plano.adicionarTrecho(R8, alt(30_000), as(9, 26), null);
        plano.adicionarTrecho(R1, alt(altitudeDoSegundoR1), as(9, 52), null);
        plano.adicionarTrecho(R4, alt(30_000), as(10, 18), new Pouso(vaga(CWB, 1), 30));
        return plano;
    }

    @Test
    void mesmaAltitudeDaMesmaAeroviaEmDoisTrechosDoPlanoNoMesmoSlotFalha() {
        PlanoDeVoo plano = planoQueRepeteR1(30_000);

        assertThat(plano.validar(TUDO_LIVRE).violacoes()).singleElement().satisfies(v -> {
            assertThat(v.codigo()).isEqualTo(CodigoViolacao.ALTITUDE_OCUPADA);
            assertThat(v.trecho()).isEqualTo(3);
            assertThat(v.mensagem()).isEqualTo(
                    "Altitude 30000 de R1 já usada pelo trecho 1 deste plano no slot 2026-12-01T09:00.");
        });
        assertThatThrownBy(() -> plano.aprovar(TUDO_LIVRE)).isInstanceOf(PlanoInvalidoException.class);
        assertThat(plano.getSituacao()).isEqualTo(SituacaoPlano.EM_EDICAO);
        assertThat(plano.getReservas()).isEmpty();
    }

    @Test
    void mesmaAeroviaEmOutraAltitudeNaoConflita() {
        assertThat(planoQueRepeteR1(31_000).validar(TUDO_LIVRE).valido()).isTrue();
    }

    @Test
    void conflitoInternoEConflitoComOBancoConvivemNoMesmoSlot() {
        Ocupacao ocupacao = new Ocupacao().ocuparAltitude(R1, 30_000, as(9, 0));
        // trecho 1: reservado no banco; trecho 3: reservado no banco e usado pelo trecho 1
        assertThat(planoQueRepeteR1(30_000).validar(ocupacao).violacoes())
                .extracting(v -> v.codigo() + "@" + v.trecho())
                .containsExactly("ALTITUDE_OCUPADA@1", "ALTITUDE_OCUPADA@3", "ALTITUDE_OCUPADA@3");
    }

    /**
     * A malha permite pousar duas vezes em FLN dentro da mesma hora com PR-AZA (850 km/h):
     * R1 07:30→07:56, pouso FLN-1 por 5 min (vaga em [07:56, 08:01): slots 07h e 08h);
     * R4 08:01→08:19 e R5 08:19→08:37 com sobrevoo de CWB; pouso FLN de novo às 08:37 (slots 08h e 09h).
     */
    private PlanoDeVoo planoQuePousaDuasVezesEmFln(int vagaDoSegundoPouso) {
        PlanoDeVoo plano = plano(PASSAGEIROS, POA, FLN, as(7, 30));
        plano.adicionarTrecho(R1, alt(30_000), as(7, 30), new Pouso(vaga(FLN, 1), 5));
        plano.adicionarTrecho(R4, alt(30_000), as(8, 1), null);
        plano.adicionarTrecho(R5, alt(30_000), as(8, 19), new Pouso(vaga(FLN, vagaDoSegundoPouso), 30));
        return plano;
    }

    @Test
    void mesmaVagaEmDoisPousosDoPlanoNoMesmoSlotFalha() {
        assertThat(planoQuePousaDuasVezesEmFln(1).validar(TUDO_LIVRE).violacoes()).singleElement().satisfies(v -> {
            assertThat(v.codigo()).isEqualTo(CodigoViolacao.VAGA_OCUPADA);
            assertThat(v.trecho()).isEqualTo(3);
            assertThat(v.mensagem()).isEqualTo(
                    "Vaga FLN-1 já usada pelo pouso do trecho 1 deste plano no slot 2026-12-01T08:00.");
        });
    }

    @Test
    void outraVagaNoMesmoAeroportoNaoConflita() {
        assertThat(planoQuePousaDuasVezesEmFln(2).validar(TUDO_LIVRE).valido()).isTrue();
    }

    @Test
    void resultadoAcumulaTodasAsViolacoes() {
        // Regra 10: altitude proibida + horário incompatível + sem pouso final + altitude ocupada
        PlanoDeVoo plano = plano(PASSAGEIROS, POA, CWB, as(14, 0));
        plano.adicionarTrecho(R1, alt(27_000), as(14, 0), null);
        plano.adicionarTrecho(R4, alt(30_000), as(14, 0), null);
        Ocupacao ocupacao = new Ocupacao().ocuparAltitude(R4, 30_000, as(14, 0));
        ResultadoValidacao resultado = plano.validar(ocupacao);
        assertThat(resultado.valido()).isFalse();
        assertThat(resultado.violacoes()).extracting(v -> v.codigo()).contains(
                CodigoViolacao.ALTITUDE_PROIBIDA_CATEGORIA,
                CodigoViolacao.HORARIO_INCOMPATIVEL,
                CodigoViolacao.SEM_POUSO_FINAL,
                CodigoViolacao.ALTITUDE_OCUPADA);
    }
}
