package br.pucrs.projarc.gestaoplanosvoo.application.usecases;

import br.pucrs.projarc.gestaoplanosvoo.application.dto.response.AeronaveDetalheResponse;
import br.pucrs.projarc.gestaoplanosvoo.application.dto.response.AeroportoResponse;
import br.pucrs.projarc.gestaoplanosvoo.application.dto.response.AeroviaResponse;
import br.pucrs.projarc.gestaoplanosvoo.application.dto.response.PilotoResponse;
import br.pucrs.projarc.gestaoplanosvoo.domain.exception.RecursoNaoEncontradoException;
import br.pucrs.projarc.gestaoplanosvoo.domain.repository.MalhaRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** UC01: consulta da rede aérea (aeroportos e aerovias) e dos recursos (pilotos e aeronaves). */
@Service
@Transactional(readOnly = true)
public class ConsultarMalhaUseCase {

    private final MalhaRepositoryPort malha;

    public ConsultarMalhaUseCase(MalhaRepositoryPort malha) {
        this.malha = malha;
    }

    public List<AeroportoResponse> aeroportos() {
        return malha.listarAeroportos().stream().map(AeroportoResponse::de).toList();
    }

    public AeroportoResponse aeroporto(String codigo) {
        return malha.buscarAeroporto(codigo).map(AeroportoResponse::de)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aeroporto " + codigo + " não encontrado."));
    }

    public List<AeroviaResponse> aerovias() {
        return malha.listarAerovias().stream().map(AeroviaResponse::de).toList();
    }

    public AeroviaResponse aerovia(String id) {
        return malha.buscarAerovia(id).map(AeroviaResponse::de)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aerovia " + id + " não encontrada."));
    }

    public List<PilotoResponse> pilotos() {
        return malha.listarPilotos().stream().map(PilotoResponse::de).toList();
    }

    public List<AeronaveDetalheResponse> aeronaves() {
        return malha.listarAeronaves().stream().map(AeronaveDetalheResponse::de).toList();
    }

    public AeronaveDetalheResponse aeronave(String prefixo) {
        return malha.buscarAeronave(prefixo).map(AeronaveDetalheResponse::de)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aeronave " + prefixo + " não encontrada."));
    }
}
