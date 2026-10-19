package br.pucrs.projarc.gestaoplanosvoo.domain;

import br.pucrs.projarc.gestaoplanosvoo.domain.model.Aerovia;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.CodigoViolacao;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.PlanoDeVoo;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Pouso;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.ResultadoValidacao;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.aeronave.AeronaveParticular;
import org.junit.jupiter.api.Test;

import static br.pucrs.projarc.gestaoplanosvoo.domain.Malha.*;
import static org.assertj.core.api.Assertions.assertThat;

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

    @Test
    void distanciaExatamenteNoLimiteDe85PorCentoPassa() {
        // autonomia de 1.000 km -> limite seguro de 850 km; aerovia de teste com exatamente 850 km
        var aeronave = new AeronaveParticular("PP-TST", 850, 1000, "Teste");
        var r850 = new Aerovia("RX", POA, FLN, 850);
        PlanoDeVoo plano = plano(aeronave, POA, FLN, as(9, 0));
        plano.adicionarTrecho(r850, alt(25_000), as(9, 0), new Pouso(vaga(FLN, 1), 30));
        assertThat(plano.validar(TUDO_LIVRE).contem(CodigoViolacao.AUTONOMIA_EXCEDIDA)).isFalse();

        var r851 = new Aerovia("RY", POA, FLN, 851);
        PlanoDeVoo acima = plano(aeronave, POA, FLN, as(9, 0));
        acima.adicionarTrecho(r851, alt(25_000), as(9, 0), new Pouso(vaga(FLN, 1), 30));
        assertThat(acima.validar(TUDO_LIVRE).contem(CodigoViolacao.AUTONOMIA_EXCEDIDA)).isTrue();
    }
}
