package br.pucrs.projarc.gestaoplanosvoo.domain.model;

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

    public static List<SlotHorario> cobrindo(LocalDateTime ini, LocalDateTime fim) {
        List<SlotHorario> slots = new ArrayList<>();
        for (LocalDateTime h = ini.truncatedTo(ChronoUnit.HOURS); h.isBefore(fim); h = h.plusHours(1)) {
            slots.add(new SlotHorario(h));
        }
        return slots;
    }
}
