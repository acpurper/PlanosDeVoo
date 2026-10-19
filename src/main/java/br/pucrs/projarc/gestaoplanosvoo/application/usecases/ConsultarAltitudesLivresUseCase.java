package br.pucrs.projarc.gestaoplanosvoo.application.usecases;

import br.pucrs.projarc.gestaoplanosvoo.application.dto.response.DisponibilidadeAeroviaResponse;
import br.pucrs.projarc.gestaoplanosvoo.application.dto.response.SlotOcupadoResponse;
import br.pucrs.projarc.gestaoplanosvoo.domain.exception.RecursoNaoEncontradoException;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Aerovia;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Altitude;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.SlotHorario;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.reserva.ReservaAerovia;
import br.pucrs.projarc.gestaoplanosvoo.domain.repository.MalhaRepositoryPort;
import br.pucrs.projarc.gestaoplanosvoo.domain.repository.ReservaRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** UC04: altitudes livres de uma aerovia num período. Retrata o momento da consulta e não reserva nada. */
@Service
public class ConsultarAltitudesLivresUseCase {

    private final MalhaRepositoryPort malha;
    private final ReservaRepositoryPort reservas;

    public ConsultarAltitudesLivresUseCase(MalhaRepositoryPort malha, ReservaRepositoryPort reservas) {
        this.malha = malha;
        this.reservas = reservas;
    }

    @Transactional(readOnly = true)
    public DisponibilidadeAeroviaResponse executar(String aeroviaId, LocalDateTime inicio, LocalDateTime fim) {
        Aerovia aerovia = malha.buscarAerovia(aeroviaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aerovia " + aeroviaId + " não encontrada."));
        List<SlotHorario> slots = SlotHorario.doPeriodo(inicio, fim);

        Map<Integer, List<ReservaAerovia>> porAltitude = reservas
                .listarDaAerovia(aeroviaId, slots.getFirst().inicio(), slots.getLast().fim()).stream()
                .collect(Collectors.groupingBy(r -> r.getAltitude().pes()));

        List<Integer> livres = new ArrayList<>();
        List<DisponibilidadeAeroviaResponse.AltitudeOcupada> ocupadas = new ArrayList<>();
        for (Altitude altitude : Altitude.todas()) {
            List<ReservaAerovia> doNivel = porAltitude.getOrDefault(altitude.pes(), List.of());
            if (doNivel.isEmpty()) {
                livres.add(altitude.pes());
            } else {
                ocupadas.add(new DisponibilidadeAeroviaResponse.AltitudeOcupada(altitude.pes(), doNivel.stream()
                        .map(r -> new SlotOcupadoResponse(r.getSlot().inicio(), r.getPlanoId())).toList()));
            }
        }
        return new DisponibilidadeAeroviaResponse(aerovia.getId(), aerovia.getOrigem().getCodigo(),
                aerovia.getDestino().getCodigo(), inicio, fim, slots.stream().map(SlotHorario::inicio).toList(),
                livres, ocupadas);
    }
}
