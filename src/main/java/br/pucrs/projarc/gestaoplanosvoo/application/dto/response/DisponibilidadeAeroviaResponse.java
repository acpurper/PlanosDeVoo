package br.pucrs.projarc.gestaoplanosvoo.application.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Situação de uma aerovia no período consultado. Uma altitude é disponível quando está livre em
 * todos os slots do período. A consulta não reserva nada.
 */
public record DisponibilidadeAeroviaResponse(String aerovia, String origem, String destino, LocalDateTime inicio,
                                             LocalDateTime fim, List<LocalDateTime> slots,
                                             List<Integer> altitudesDisponiveis,
                                             List<AltitudeOcupada> altitudesOcupadas) {

    public record AltitudeOcupada(int altitudePes, List<SlotOcupadoResponse> slotsOcupados) {
    }
}
