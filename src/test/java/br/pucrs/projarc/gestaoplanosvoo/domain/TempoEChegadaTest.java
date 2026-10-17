package br.pucrs.projarc.gestaoplanosvoo.domain;

import br.pucrs.projarc.gestaoplanosvoo.domain.model.Pouso;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Trecho;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static br.pucrs.projarc.gestaoplanosvoo.domain.Malha.*;
import static org.assertj.core.api.Assertions.assertThat;

class TempoEChegadaTest {

    @Test
    void duracaoFracionadaArredondaParaCima() {
        // 363 km a 600 km/h = 36,3 min -> 37 min
        assertThat(R1.duracao(600)).isEqualTo(Duration.ofMinutes(37));
        // 865 km a 850 km/h = 61,06 min -> 62 min
        assertThat(R2.duracao(850)).isEqualTo(Duration.ofMinutes(62));
    }

    @Test
    void duracaoExataNaoArredonda() {
        // 360 km a 600 km/h = 36 min exatos
        assertThat(R6.duracao(600)).isEqualTo(Duration.ofMinutes(36));
    }

    @Test
    void chegadaEhEntradaMaisDuracao() {
        Trecho trecho = new Trecho(1, R1, alt(26_000), as(14, 0), null);
        assertThat(trecho.horarioChegada(600)).isEqualTo(as(14, 37));
    }

    @Test
    void liberacaoAposPousoSomaPermanencia() {
        Trecho sobrevoo = new Trecho(1, R1, alt(26_000), as(14, 0), null);
        Trecho pouso = new Trecho(1, R1, alt(26_000), as(14, 0), new Pouso(vaga(FLN, 1), 30));
        assertThat(sobrevoo.horarioLiberacao(600)).isEqualTo(as(14, 37));
        assertThat(pouso.horarioLiberacao(600)).isEqualTo(as(15, 7));
    }

    @Test
    void entradaComSegundosEhTruncadaParaMinuto() {
        Trecho trecho = new Trecho(1, R1, alt(26_000), as(14, 0).withSecond(42), null);
        assertThat(trecho.getHorarioEntrada()).isEqualTo(as(14, 0));
    }
}
