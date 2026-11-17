package br.pucrs.projarc.gestaoplanosvoo.application.usecases;

import br.pucrs.projarc.gestaoplanosvoo.application.dto.response.ResultadoValidacaoResponse;
import br.pucrs.projarc.gestaoplanosvoo.domain.exception.RecursoNaoEncontradoException;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.PlanoDeVoo;
import br.pucrs.projarc.gestaoplanosvoo.domain.repository.PlanoDeVooRepositoryPort;
import br.pucrs.projarc.gestaoplanosvoo.domain.service.GestaoReservas;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** UC06: valida o plano com a ocupação atual, sem gravar nada. */
@Service
public class ValidarPlanoUseCase {

    private final PlanoDeVooRepositoryPort planos;
    private final GestaoReservas reservas;

    public ValidarPlanoUseCase(PlanoDeVooRepositoryPort planos, GestaoReservas reservas) {
        this.planos = planos;
        this.reservas = reservas;
    }

    @Transactional(readOnly = true)
    public ResultadoValidacaoResponse executar(Long planoId) {
        PlanoDeVoo plano = planos.buscarPorId(planoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Plano " + planoId + " não encontrado."));
        return ResultadoValidacaoResponse.de(planoId, plano.validar(reservas));
    }
}
