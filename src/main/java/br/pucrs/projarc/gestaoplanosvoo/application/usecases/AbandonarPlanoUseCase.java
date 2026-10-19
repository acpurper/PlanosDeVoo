package br.pucrs.projarc.gestaoplanosvoo.application.usecases;

import br.pucrs.projarc.gestaoplanosvoo.application.dto.response.PlanoResponse;
import br.pucrs.projarc.gestaoplanosvoo.domain.exception.RecursoNaoEncontradoException;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.PlanoDeVoo;
import br.pucrs.projarc.gestaoplanosvoo.domain.repository.PlanoDeVooRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** UC10: um plano em edição passa a abandonado. Ele não tinha reservas, então nada é liberado. */
@Service
public class AbandonarPlanoUseCase {

    private final PlanoDeVooRepositoryPort planos;

    public AbandonarPlanoUseCase(PlanoDeVooRepositoryPort planos) {
        this.planos = planos;
    }

    @Transactional
    public PlanoResponse executar(Long planoId) {
        // O bloqueio impede que uma aprovação simultânea do mesmo plano termine depois do abandono.
        PlanoDeVoo plano = planos.buscarPorIdParaAtualizacao(planoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Plano " + planoId + " não encontrado."));
        plano.abandonar();
        return PlanoResponse.de(planos.salvar(plano));
    }
}
