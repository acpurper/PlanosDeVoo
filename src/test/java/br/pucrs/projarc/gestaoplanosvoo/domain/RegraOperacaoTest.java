package br.pucrs.projarc.gestaoplanosvoo.domain;

import br.pucrs.projarc.gestaoplanosvoo.domain.model.CodigoViolacao;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.PlanoDeVoo;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Pouso;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.ResultadoValidacao;
import org.junit.jupiter.api.Test;

import static br.pucrs.projarc.gestaoplanosvoo.domain.Malha.*;
import static org.assertj.core.api.Assertions.assertThat;

/** Regra 5: cada categoria tem sua estratégia de operação. */
class RegraOperacaoTest {

    @Test
    void particularSomenteEntre25e27MilPes() {
        assertThat(PARTICULAR.podeOperar(alt(25_000), as(10, 0), as(11, 0))).isTrue();
        assertThat(PARTICULAR.podeOperar(alt(27_000), as(10, 0), as(11, 0))).isTrue();
        assertThat(PARTICULAR.podeOperar(alt(28_000), as(10, 0), as(11, 0))).isFalse();
    }

    @Test
    void passageirosA28MilPesOuMais() {
        assertThat(PASSAGEIROS.podeOperar(alt(28_000), as(10, 0), as(11, 0))).isTrue();
        assertThat(PASSAGEIROS.podeOperar(alt(35_000), as(10, 0), as(11, 0))).isTrue();
        assertThat(PASSAGEIROS.podeOperar(alt(27_000), as(10, 0), as(11, 0))).isFalse();
    }

    @Test
    void cargaSomenteEntreMeiaNoiteESeisHoras() {
        assertThat(CARGA.podeOperar(alt(25_000), as(0, 0), as(0, 30))).isTrue();
        assertThat(CARGA.podeOperar(alt(25_000), as(5, 40), as(6, 0))).as("terminar às 06:00 é permitido").isTrue();
        assertThat(CARGA.podeOperar(alt(25_000), as(5, 45), as(6, 4))).as("o trecho inteiro precisa caber").isFalse();
        assertThat(CARGA.podeOperar(alt(25_000), as(6, 0), as(6, 30))).isFalse();
        assertThat(CARGA.podeOperar(alt(25_000), as(23, 0), as(23, 30))).isFalse();
    }

    @Test
    void violacaoDeCategoriaApareceNaValidacao() {
        PlanoDeVoo plano = plano(PASSAGEIROS, POA, FLN, as(10, 0));
        plano.adicionarTrecho(R1, alt(27_000), as(10, 0), new Pouso(vaga(FLN, 1), 30));
        ResultadoValidacao resultado = plano.validar(TUDO_LIVRE);
        assertThat(resultado.violacoes()).singleElement()
                .satisfies(v -> {
                    assertThat(v.codigo()).isEqualTo(CodigoViolacao.ALTITUDE_PROIBIDA_CATEGORIA);
                    assertThat(v.trecho()).isEqualTo(1);
                });
    }

    @Test
    void cargaForaDaJanelaApareceNaValidacao() {
        // R4 = 245 km a 780 km/h = 19 min: 05:45 -> 06:04
        PlanoDeVoo plano = plano(CARGA, FLN, CWB, as(5, 45));
        plano.adicionarTrecho(R4, alt(30_000), as(5, 45), new Pouso(vaga(CWB, 1), 30));
        assertThat(plano.validar(TUDO_LIVRE).contem(CodigoViolacao.HORARIO_PROIBIDO_CATEGORIA)).isTrue();

        PlanoDeVoo dentro = plano(CARGA, FLN, CWB, as(5, 40));
        dentro.adicionarTrecho(R4, alt(30_000), as(5, 40), new Pouso(vaga(CWB, 1), 30));
        assertThat(dentro.validar(TUDO_LIVRE).valido()).isTrue();
    }
}
