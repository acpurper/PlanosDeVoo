package br.pucrs.projarc.gestaoplanosvoo.application.usecases;

import br.pucrs.projarc.gestaoplanosvoo.application.dto.response.PlanoResponse;
import br.pucrs.projarc.gestaoplanosvoo.domain.exception.RecursoNaoEncontradoException;
import br.pucrs.projarc.gestaoplanosvoo.domain.repository.PlanoDeVooRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Suporte mínimo para demonstrar os UCs desta entrega (o UC08 completo fica para a próxima). */
@Service
public class ConsultarPlanoUseCase {

    private final PlanoDeVooRepositoryPort planos;

    public ConsultarPlanoUseCase(PlanoDeVooRepositoryPort planos) {
        this.planos = planos;
    }

    @Transactional(readOnly = true)
    public PlanoResponse executar(Long planoId) {
        return planos.buscarPorId(planoId).map(PlanoResponse::de)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Plano " + planoId + " não encontrado."));
    }
}
