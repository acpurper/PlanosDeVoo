package br.pucrs.projarc.gestaoplanosvoo.application.usecases;

import br.pucrs.projarc.gestaoplanosvoo.application.dto.request.CriarPlanoRequest;
import br.pucrs.projarc.gestaoplanosvoo.application.dto.response.PlanoResponse;
import br.pucrs.projarc.gestaoplanosvoo.domain.exception.RecursoNaoEncontradoException;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Aeroporto;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Piloto;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.PlanoDeVoo;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.aeronave.Aeronave;
import br.pucrs.projarc.gestaoplanosvoo.domain.repository.MalhaRepositoryPort;
import br.pucrs.projarc.gestaoplanosvoo.domain.repository.PlanoDeVooRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** UC02 — Criar plano de voo: nasce EM_EDICAO e sem trechos. */
@Service
public class CriarPlanoUseCase {

    private final MalhaRepositoryPort malha;
    private final PlanoDeVooRepositoryPort planos;

    public CriarPlanoUseCase(MalhaRepositoryPort malha, PlanoDeVooRepositoryPort planos) {
        this.malha = malha;
        this.planos = planos;
    }

    @Transactional
    public PlanoResponse executar(CriarPlanoRequest req) {
        Piloto piloto = malha.buscarPiloto(req.pilotoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Piloto " + req.pilotoId() + " não encontrado."));
        Aeronave aeronave = malha.buscarAeronave(req.aeronave())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aeronave " + req.aeronave() + " não encontrada."));
        Aeroporto origem = aeroporto(req.origem());
        Aeroporto destino = aeroporto(req.destino());
        PlanoDeVoo plano = PlanoDeVoo.criar(piloto, aeronave, origem, destino, req.dataHoraPartida());
        return PlanoResponse.de(planos.salvar(plano));
    }

    private Aeroporto aeroporto(String codigo) {
        return malha.buscarAeroporto(codigo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aeroporto " + codigo + " não encontrado."));
    }
}
