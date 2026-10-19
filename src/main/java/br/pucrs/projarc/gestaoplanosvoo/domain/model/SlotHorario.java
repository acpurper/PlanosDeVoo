package br.pucrs.projarc.gestaoplanosvoo.domain.model;

import br.pucrs.projarc.gestaoplanosvoo.domain.exception.RegraDeNegocioException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public record SlotHorario(LocalDateTime inicio) {

    public SlotHorario {
        if (!inicio.truncatedTo(ChronoUnit.HOURS).equals(inicio)) {
            throw new IllegalArgumentException("Slot precisa começar na hora cheia: " + inicio);
        }
    }

    public LocalDateTime fim() {
        return inicio.plusHours(1);
    }

    /** Slots de um período informado pelo usuário, que precisa terminar depois de começar. */
    public static List<SlotHorario> doPeriodo(LocalDateTime ini, LocalDateTime fim) {
        if (!fim.isAfter(ini)) {
            throw new RegraDeNegocioException("PERIODO_INVALIDO",
                    "O fim do período (" + fim + ") precisa ser posterior ao início (" + ini + ").");
        }
        return cobrindo(ini, fim);
    }

    public static List<SlotHorario> cobrindo(LocalDateTime ini, LocalDateTime fim) {
        List<SlotHorario> slots = new ArrayList<>();
        for (LocalDateTime h = ini.truncatedTo(ChronoUnit.HOURS); h.isBefore(fim); h = h.plusHours(1)) {
            slots.add(new SlotHorario(h));
        }
        return slots;
    }
}
