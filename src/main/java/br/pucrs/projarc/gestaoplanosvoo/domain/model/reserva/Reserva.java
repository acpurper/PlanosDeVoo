package br.pucrs.projarc.gestaoplanosvoo.domain.model.reserva;

import br.pucrs.projarc.gestaoplanosvoo.domain.model.SlotHorario;

import java.util.Objects;

/** Ocupação efetiva de um recurso num slot, criada somente na aprovação do plano. */
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

    /** Descrição do recurso reservado, para mensagens e respostas. */
    public abstract String recurso();
}
