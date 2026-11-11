package br.pucrs.projarc.gestaoplanosvoo.application.usecases;

import br.pucrs.projarc.gestaoplanosvoo.application.dto.response.ResultadoValidacaoResponse;
import br.pucrs.projarc.gestaoplanosvoo.domain.exception.RecursoNaoEncontradoException;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.PlanoDeVoo;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.ResultadoValidacao;
import br.pucrs.projarc.gestaoplanosvoo.domain.repository.PlanoDeVooRepositoryPort;
import br.pucrs.projarc.gestaoplanosvoo.domain.service.ReservaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ValidarPlanoUseCase {

    private final PlanoDeVooRepositoryPort planos;
    private final ReservaService reservaService;

    public ValidarPlanoUseCase(PlanoDeVooRepositoryPort planos, ReservaService reservaService) {
        this.planos = planos;
        this.reservaService = reservaService;
    }

    @Transactional(readOnly = true)
    public ResultadoValidacaoResponse executar(Long planoId) {
        PlanoDeVoo plano = planos.buscarPorId(planoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Plano " + planoId + " não encontrado."));
        return ResultadoValidacaoResponse.de(planoId, validar(plano));
    }

    ResultadoValidacao validar(PlanoDeVoo plano) {
        return plano.validar(reservaService);
    }
}
