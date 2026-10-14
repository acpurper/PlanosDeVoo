package br.pucrs.projarc.gestaoplanosvoo.domain;

import br.pucrs.projarc.gestaoplanosvoo.domain.model.CodigoViolacao;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.PlanoDeVoo;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Pouso;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.ResultadoValidacao;
import org.junit.jupiter.api.Test;

import static br.pucrs.projarc.gestaoplanosvoo.domain.Malha.*;
import static org.assertj.core.api.Assertions.assertThat;

/** Regra 6: distância desde o último pouso <= 85% da autonomia. PP-LEN: 700 km -> 595 km. */
class AutonomiaTest {

    @Test
    void autonomiaSeguraEh85PorCento() {
        assertThat(PARTICULAR_CURTA.autonomiaSegura()).isEqualTo(595.0);
    }

    @Test
    void trechoDentroDoLimitePassa() {
        // POA -> FLN: 363 km; 400 km/h -> 55 min
        PlanoDeVoo plano = plano(PARTICULAR_CURTA, POA, FLN, as(9, 0));
        plano.adicionarTrecho(R1, alt(25_000), as(9, 0), new Pouso(vaga(FLN, 1), 30));
        assertThat(plano.validar(TUDO_LIVRE).valido()).isTrue();
    }

    @Test
    void sobrevooAcumulaDistancia() {
        // POA -> FLN (sobrevoo) -> CWB: 363 + 245 = 608 km > 595 km
        PlanoDeVoo plano = plano(PARTICULAR_CURTA, POA, CWB, as(9, 0));
        plano.adicionarTrecho(R1, alt(25_000), as(9, 0), null);
        plano.adicionarTrecho(R4, alt(25_000), as(9, 55), new Pouso(vaga(CWB, 1), 30));
        ResultadoValidacao resultado = plano.validar(TUDO_LIVRE);
        assertThat(resultado.violacoes()).singleElement()
                .satisfies(v -> {
                    assertThat(v.codigo()).isEqualTo(CodigoViolacao.AUTONOMIA_EXCEDIDA);
                    assertThat(v.trecho()).isEqualTo(2);
                });
    }

    @Test
    void pousoReiniciaOControle() {
        // mesmo percurso com pouso em FLN: 363 km e depois 245 km
        PlanoDeVoo plano = plano(PARTICULAR_CURTA, POA, CWB, as(9, 0));
        plano.adicionarTrecho(R1, alt(25_000), as(9, 0), new Pouso(vaga(FLN, 1), 30));
        plano.adicionarTrecho(R4, alt(25_000), as(10, 25), new Pouso(vaga(CWB, 1), 30));
        assertThat(plano.validar(TUDO_LIVRE).valido()).isTrue();
        assertThat(plano.distanciasDesdeAbastecimento()).containsExactly(363.0, 245.0);
    }

    @Test
    void trechoUnicoMaiorQueOLimiteFalha() {
        // POA -> GRU: 865 km > 595 km
        PlanoDeVoo plano = plano(PARTICULAR_CURTA, POA, GRU, as(9, 0));
        plano.adicionarTrecho(R2, alt(25_000), as(9, 0), new Pouso(vaga(GRU, 1), 30));
        assertThat(plano.validar(TUDO_LIVRE).contem(CodigoViolacao.AUTONOMIA_EXCEDIDA)).isTrue();
    }
}
