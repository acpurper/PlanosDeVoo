package br.pucrs.projarc.gestaoplanosvoo.application.usecases;

import br.pucrs.projarc.gestaoplanosvoo.application.dto.response.ResultadoValidacaoResponse;
import br.pucrs.projarc.gestaoplanosvoo.domain.exception.RecursoNaoEncontradoException;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.PlanoDeVoo;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.ResultadoValidacao;
import br.pucrs.projarc.gestaoplanosvoo.domain.repository.PlanoDeVooRepositoryPort;
import br.pucrs.projarc.gestaoplanosvoo.domain.repository.ReservaRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** UC06 — Validar plano: não muda a situação e não grava nada. */
@Service
public class ValidarPlanoUseCase {

    private final PlanoDeVooRepositoryPort planos;
    private final ReservaRepositoryPort reservas;

    public ValidarPlanoUseCase(PlanoDeVooRepositoryPort planos, ReservaRepositoryPort reservas) {
        this.planos = planos;
        this.reservas = reservas;
    }

    @Transactional(readOnly = true)
    public ResultadoValidacaoResponse executar(Long planoId) {
        PlanoDeVoo plano = planos.buscarPorId(planoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Plano " + planoId + " não encontrado."));
        return ResultadoValidacaoResponse.de(planoId, validar(plano));
    }

    /** Lógica de validação compartilhada com a aprovação (UC07 «include» UC06). */
    ResultadoValidacao validar(PlanoDeVoo plano) {
        return plano.validar(reservas);
    }
}
