package br.pucrs.projarc.gestaoplanosvoo.domain.model;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Janela de uma hora alinhada à hora cheia: [inicio, inicio + 1h).
 * O uso de um recurso é o intervalo semiaberto [ini, fim); o slot [h, h+1h) fica ocupado se h < fim e h+1h > ini.
 */
public record SlotHorario(LocalDateTime inicio) {

    public SlotHorario {
        if (!inicio.truncatedTo(ChronoUnit.HOURS).equals(inicio)) {
            throw new IllegalArgumentException("Slot precisa começar na hora cheia: " + inicio);
        }
    }

    public LocalDateTime fim() {
        return inicio.plusHours(1);
    }

    /** Todos os slots ocupados pelo intervalo [ini, fim). Intervalo vazio não ocupa slot. */
    public static List<SlotHorario> cobrindo(LocalDateTime ini, LocalDateTime fim) {
        List<SlotHorario> slots = new ArrayList<>();
        for (LocalDateTime h = ini.truncatedTo(ChronoUnit.HOURS); h.isBefore(fim); h = h.plusHours(1)) {
            slots.add(new SlotHorario(h));
        }
        return slots;
    }
}
