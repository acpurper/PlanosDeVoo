package br.pucrs.projarc.gestaoplanosvoo.domain.model.reserva;

import br.pucrs.projarc.gestaoplanosvoo.domain.model.SlotHorario;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Vaga;

/** Ocupa uma vaga num slot. Único por (vaga, slot). */
public final class ReservaVaga extends Reserva {

    private final Vaga vaga;

    public ReservaVaga(Long id, Long planoId, SlotHorario slot, Vaga vaga) {
        super(id, planoId, slot);
        this.vaga = vaga;
    }

    public Vaga getVaga() { return vaga; }

    @Override
    public String recurso() {
        return "vaga " + vaga;
    }
}
