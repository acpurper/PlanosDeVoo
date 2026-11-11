package br.pucrs.projarc.gestaoplanosvoo.application.usecases;

import br.pucrs.projarc.gestaoplanosvoo.application.dto.response.PlanoResponse;
import br.pucrs.projarc.gestaoplanosvoo.domain.exception.RecursoNaoEncontradoException;
import br.pucrs.projarc.gestaoplanosvoo.domain.repository.PlanoDeVooRepositoryPort;
import br.pucrs.projarc.gestaoplanosvoo.domain.service.ReservaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultarPlanoUseCase {

    private final PlanoDeVooRepositoryPort planos;
    private final ReservaService reservaService;

    public ConsultarPlanoUseCase(PlanoDeVooRepositoryPort planos, ReservaService reservaService) {
        this.planos = planos;
        this.reservaService = reservaService;
    }

    @Transactional(readOnly = true)
    public PlanoResponse executar(Long planoId) {
        return planos.buscarPorId(planoId)
                .map(p -> PlanoResponse.de(p, reservaService.doPlano(planoId)))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Plano " + planoId + " não encontrado."));
    }
}
