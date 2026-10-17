package br.pucrs.projarc.gestaoplanosvoo.domain;

import br.pucrs.projarc.gestaoplanosvoo.domain.model.CodigoViolacao;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.PlanoDeVoo;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Pouso;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.ResultadoValidacao;
import org.junit.jupiter.api.Test;

import static br.pucrs.projarc.gestaoplanosvoo.domain.Malha.*;
import static org.assertj.core.api.Assertions.assertThat;

class PercursoTest {

    private PlanoDeVoo planoValido() {
        PlanoDeVoo plano = plano(PARTICULAR, POA, CWB, as(14, 0));
        plano.adicionarTrecho(R1, alt(26_000), as(14, 0), null);
        plano.adicionarTrecho(R4, alt(26_000), as(14, 37), new Pouso(vaga(CWB, 1), 45));
        return plano;
    }

    @Test
    void percursoCoerenteEhValido() {
        assertThat(planoValido().validar(TUDO_LIVRE).valido()).isTrue();
    }

    @Test
    void semTrechosEhInvalido() {
        PlanoDeVoo plano = plano(PARTICULAR, POA, CWB, as(14, 0));
        assertThat(plano.validar(TUDO_LIVRE).violacoes())
                .extracting(v -> v.codigo()).containsExactly(CodigoViolacao.PERCURSO_VAZIO);
    }

    @Test
    void primeiraAeroviaPrecisaSairDaOrigem() {
        PlanoDeVoo plano = plano(PARTICULAR, POA, CWB, as(14, 0));
        plano.adicionarTrecho(R4, alt(26_000), as(14, 0), new Pouso(vaga(CWB, 1), 45));
        assertThat(plano.validar(TUDO_LIVRE).contem(CodigoViolacao.ORIGEM_INCOERENTE)).isTrue();
    }

    @Test
    void ultimaAeroviaPrecisaChegarAoDestino() {
        PlanoDeVoo plano = plano(PARTICULAR, POA, CWB, as(14, 0));
        plano.adicionarTrecho(R1, alt(26_000), as(14, 0), new Pouso(vaga(FLN, 1), 45));
        assertThat(plano.validar(TUDO_LIVRE).contem(CodigoViolacao.DESTINO_INCOERENTE)).isTrue();
    }

    @Test
    void aeroviasPrecisamSerContinuas() {
        // R1 termina em FLN, R6 começa em CWB
        PlanoDeVoo plano = plano(PARTICULAR, POA, GRU, as(14, 0));
        plano.adicionarTrecho(R1, alt(26_000), as(14, 0), null);
        plano.adicionarTrecho(R6, alt(26_000), as(14, 37), new Pouso(vaga(GRU, 1), 45));
        ResultadoValidacao resultado = plano.validar(TUDO_LIVRE);
        assertThat(resultado.violacoes()).anySatisfy(v -> {
            assertThat(v.codigo()).isEqualTo(CodigoViolacao.PERCURSO_DESCONTINUO);
            assertThat(v.trecho()).isEqualTo(2);
        });
    }

    @Test
    void primeiraEntradaPrecisaSerAPartida() {
        PlanoDeVoo plano = plano(PARTICULAR, POA, FLN, as(14, 0));
        plano.adicionarTrecho(R1, alt(26_000), as(14, 5), new Pouso(vaga(FLN, 1), 45));
        assertThat(plano.validar(TUDO_LIVRE).contem(CodigoViolacao.ENTRADA_DIFERENTE_PARTIDA)).isTrue();
    }

    @Test
    void aposSobrevooEntradaSeguintePrecisaSerAChegada() {
        PlanoDeVoo plano = plano(PARTICULAR, POA, CWB, as(14, 0));
        plano.adicionarTrecho(R1, alt(26_000), as(14, 0), null);
        plano.adicionarTrecho(R4, alt(26_000), as(14, 30), new Pouso(vaga(CWB, 1), 45));
        assertThat(plano.validar(TUDO_LIVRE).violacoes()).singleElement().satisfies(v -> {
            assertThat(v.codigo()).isEqualTo(CodigoViolacao.HORARIO_INCOMPATIVEL);
            assertThat(v.mensagem()).contains("14:37");
        });
    }

    @Test
    void aposPousoEntradaSeguintePrecisaSerChegadaMaisPermanencia() {
        PlanoDeVoo plano = plano(PARTICULAR, POA, CWB, as(14, 0));
        plano.adicionarTrecho(R1, alt(26_000), as(14, 0), new Pouso(vaga(FLN, 1), 30));
        plano.adicionarTrecho(R4, alt(26_000), as(14, 37), new Pouso(vaga(CWB, 1), 45));
        assertThat(plano.validar(TUDO_LIVRE).contem(CodigoViolacao.HORARIO_INCOMPATIVEL)).isTrue();

        PlanoDeVoo corrigido = plano(PARTICULAR, POA, CWB, as(14, 0));
        corrigido.adicionarTrecho(R1, alt(26_000), as(14, 0), new Pouso(vaga(FLN, 1), 30));
        corrigido.adicionarTrecho(R4, alt(26_000), as(15, 7), new Pouso(vaga(CWB, 1), 45));
        assertThat(corrigido.validar(TUDO_LIVRE).valido()).isTrue();
    }

    @Test
    void ultimoTrechoPrecisaTerPouso() {
        PlanoDeVoo plano = plano(PARTICULAR, POA, FLN, as(14, 0));
        plano.adicionarTrecho(R1, alt(26_000), as(14, 0), null);
        assertThat(plano.validar(TUDO_LIVRE).violacoes())
                .extracting(v -> v.codigo()).containsExactly(CodigoViolacao.SEM_POUSO_FINAL);
    }

    @Test
    void chegadaFinalPrecisaSerNoDiaDaPartida() {
        // R2 a 850 km/h = 62 min: 23:30 -> 00:32 do dia seguinte
        PlanoDeVoo plano = plano(PASSAGEIROS, POA, GRU, as(23, 30));
        plano.adicionarTrecho(R2, alt(30_000), as(23, 30), new Pouso(vaga(GRU, 1), 30));
        assertThat(plano.validar(TUDO_LIVRE).contem(CodigoViolacao.DIAS_DIFERENTES)).isTrue();
    }

    @Test
    void permanenciaFinalPodePassarDaMeiaNoite() {
        // 22:00 -> 23:02, permanência de 5 h termina no dia seguinte
        PlanoDeVoo plano = plano(PASSAGEIROS, POA, GRU, as(22, 0));
        plano.adicionarTrecho(R2, alt(30_000), as(22, 0), new Pouso(vaga(GRU, 1), 300));
        assertThat(plano.validar(TUDO_LIVRE).valido()).isTrue();
    }
}
