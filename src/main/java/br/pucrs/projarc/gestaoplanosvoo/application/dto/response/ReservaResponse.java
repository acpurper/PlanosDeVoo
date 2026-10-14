package br.pucrs.projarc.gestaoplanosvoo.application.dto.response;

import br.pucrs.projarc.gestaoplanosvoo.domain.model.reserva.Reserva;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.reserva.ReservaAerovia;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.reserva.ReservaVaga;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReservaResponse(String tipo, String aerovia, Integer altitudePes, String aeroporto, Integer vaga,
                              LocalDateTime slotInicio, LocalDateTime slotFim) {

    public static ReservaResponse de(Reserva r) {
        return switch (r) {
            case ReservaAerovia a -> new ReservaResponse("AEROVIA", a.getAerovia().getId(), a.getAltitude().pes(),
                    null, null, a.getSlot().inicio(), a.getSlot().fim());
            case ReservaVaga v -> new ReservaResponse("VAGA", null, null, v.getVaga().getAeroportoCodigo(),
                    v.getVaga().getNumero(), v.getSlot().inicio(), v.getSlot().fim());
            default -> throw new IllegalStateException("Tipo de reserva desconhecido: " + r.getClass());
        };
    }
}
