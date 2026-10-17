package br.pucrs.projarc.gestaoplanosvoo.domain;

import br.pucrs.projarc.gestaoplanosvoo.domain.model.Pouso;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.SlotHorario;
import org.junit.jupiter.api.Test;

import static br.pucrs.projarc.gestaoplanosvoo.domain.Malha.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SlotHorarioTest {

    @Test
    void exemploDoEnunciadoParaAerovia() {
        // entrada 15:52 e saída 17:07 ocupam 15h, 16h e 17h
        assertThat(SlotHorario.cobrindo(as(15, 52), as(17, 7)))
                .extracting(SlotHorario::inicio)
                .containsExactly(as(15, 0), as(16, 0), as(17, 0));
    }

    @Test
    void exemploDoEnunciadoParaVaga() {
        // chegada 15:50 com permanência de 20 min ocupa 15h e 16h
        Pouso pouso = new Pouso(vaga(POA, 1), 20);
        assertThat(pouso.slots(as(15, 50)))
                .extracting(SlotHorario::inicio)
                .containsExactly(as(15, 0), as(16, 0));
    }

    @Test
    void saidaNaHoraCheiaNaoOcupaOSlotSeguinte() {
        assertThat(SlotHorario.cobrindo(as(16, 10), as(17, 0)))
                .extracting(SlotHorario::inicio)
                .containsExactly(as(16, 0));
    }

    @Test
    void usoDentroDeUmaHoraOcupaUmSlot() {
        assertThat(SlotHorario.cobrindo(as(14, 0), as(14, 37))).hasSize(1);
    }

    @Test
    void permanenciaQuePassaDaMeiaNoiteOcupaSlotsDoDiaSeguinte() {
        Pouso pouso = new Pouso(vaga(GRU, 1), 120);
        assertThat(pouso.slots(as(23, 30)))
                .extracting(SlotHorario::inicio)
                .containsExactly(as(23, 0), DIA.plusDays(1), DIA.plusDays(1).withHour(1));
    }

    @Test
    void slotForaDaHoraCheiaEhRejeitado() {
        assertThatThrownBy(() -> new SlotHorario(as(15, 30))).isInstanceOf(IllegalArgumentException.class);
    }
}
