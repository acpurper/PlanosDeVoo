package br.pucrs.projarc.gestaoplanosvoo.application.usecases;

import br.pucrs.projarc.gestaoplanosvoo.application.dto.response.DisponibilidadeVagasResponse;
import br.pucrs.projarc.gestaoplanosvoo.application.dto.response.SlotOcupadoResponse;
import br.pucrs.projarc.gestaoplanosvoo.domain.exception.RecursoNaoEncontradoException;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Aeroporto;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.SlotHorario;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Vaga;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.reserva.ReservaVaga;
import br.pucrs.projarc.gestaoplanosvoo.domain.repository.MalhaRepositoryPort;
import br.pucrs.projarc.gestaoplanosvoo.domain.repository.ReservaRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** UC05: vagas livres de um aeroporto num período. Retrata o momento da consulta e não reserva nada. */
@Service
public class ConsultarVagasLivresUseCase {

    private final MalhaRepositoryPort malha;
    private final ReservaRepositoryPort reservas;

    public ConsultarVagasLivresUseCase(MalhaRepositoryPort malha, ReservaRepositoryPort reservas) {
        this.malha = malha;
        this.reservas = reservas;
    }

    @Transactional(readOnly = true)
    public DisponibilidadeVagasResponse executar(String aeroportoCodigo, LocalDateTime inicio, LocalDateTime fim) {
        Aeroporto aeroporto = malha.buscarAeroporto(aeroportoCodigo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aeroporto " + aeroportoCodigo + " não encontrado."));
        List<SlotHorario> slots = SlotHorario.doPeriodo(inicio, fim);

        Map<Integer, List<ReservaVaga>> porVaga = reservas
                .listarDoAeroporto(aeroportoCodigo, slots.getFirst().inicio(), slots.getLast().fim()).stream()
                .collect(Collectors.groupingBy(r -> r.getVaga().getNumero()));

        List<Integer> livres = new ArrayList<>();
        List<DisponibilidadeVagasResponse.VagaOcupada> ocupadas = new ArrayList<>();
        for (Vaga vaga : aeroporto.getVagas()) {
            List<ReservaVaga> daVaga = porVaga.getOrDefault(vaga.getNumero(), List.of());
            if (daVaga.isEmpty()) {
                livres.add(vaga.getNumero());
            } else {
                ocupadas.add(new DisponibilidadeVagasResponse.VagaOcupada(vaga.getNumero(), daVaga.stream()
                        .map(r -> new SlotOcupadoResponse(r.getSlot().inicio(), r.getPlanoId())).toList()));
            }
        }
        return new DisponibilidadeVagasResponse(aeroporto.getCodigo(), inicio, fim,
                slots.stream().map(SlotHorario::inicio).toList(), aeroporto.getVagas().size(), livres.size(),
                livres, ocupadas);
    }
}
