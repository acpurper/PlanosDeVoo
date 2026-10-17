package br.pucrs.projarc.gestaoplanosvoo.domain.model.reserva;

import br.pucrs.projarc.gestaoplanosvoo.domain.model.SlotHorario;

import java.util.Objects;

public abstract class Reserva {

    private final Long id;
    private final Long planoId;
    private final SlotHorario slot;

    protected Reserva(Long id, Long planoId, SlotHorario slot) {
        this.id = id;
        this.planoId = planoId;
        this.slot = Objects.requireNonNull(slot);
    }

    public Long getId() { return id; }
    public Long getPlanoId() { return planoId; }
    public SlotHorario getSlot() { return slot; }

    public abstract String recurso();
}
