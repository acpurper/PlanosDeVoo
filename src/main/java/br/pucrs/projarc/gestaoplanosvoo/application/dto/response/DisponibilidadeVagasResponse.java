package br.pucrs.projarc.gestaoplanosvoo.application.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Situação das vagas de um aeroporto no período consultado. Uma vaga é disponível quando está livre
 * em todos os slots do período. A consulta não reserva nada.
 */
public record DisponibilidadeVagasResponse(String aeroporto, LocalDateTime inicio, LocalDateTime fim,
                                           List<LocalDateTime> slots, int totalVagas, int quantidadeDisponivel,
                                           List<Integer> vagasDisponiveis, List<VagaOcupada> vagasOcupadas) {

    public record VagaOcupada(int vaga, List<SlotOcupadoResponse> slotsOcupados) {
    }
}
