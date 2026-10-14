package br.pucrs.projarc.gestaoplanosvoo.domain.model.reserva;

import br.pucrs.projarc.gestaoplanosvoo.domain.model.Aerovia;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Altitude;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.SlotHorario;

/** Ocupa uma altitude de uma aerovia num slot. Único por (aerovia, altitude, slot). */
public final class ReservaAerovia extends Reserva {

    private final Aerovia aerovia;
    private final Altitude altitude;

    public ReservaAerovia(Long id, Long planoId, SlotHorario slot, Aerovia aerovia, Altitude altitude) {
        super(id, planoId, slot);
        this.aerovia = aerovia;
        this.altitude = altitude;
    }

    public Aerovia getAerovia() { return aerovia; }
    public Altitude getAltitude() { return altitude; }

    @Override
    public String recurso() {
        return aerovia.getId() + " @ " + altitude.pes() + " pés";
    }
}
