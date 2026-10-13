package br.pucrs.projarc.gestaoplanosvoo.domain;

import br.pucrs.projarc.gestaoplanosvoo.domain.model.CodigoViolacao;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.PlanoDeVoo;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Pouso;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.ResultadoValidacao;
import br.pucrs.projarc.gestaoplanosvoo.domain.exception.RegraDeNegocioException;
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
