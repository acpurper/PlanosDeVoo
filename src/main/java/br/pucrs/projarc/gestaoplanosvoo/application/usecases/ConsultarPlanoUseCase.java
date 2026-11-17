package br.pucrs.projarc.gestaoplanosvoo.application.usecases;

import br.pucrs.projarc.gestaoplanosvoo.application.dto.response.PlanoResponse;
import br.pucrs.projarc.gestaoplanosvoo.domain.exception.RecursoNaoEncontradoException;
import br.pucrs.projarc.gestaoplanosvoo.domain.repository.PlanoDeVooRepositoryPort;
import br.pucrs.projarc.gestaoplanosvoo.domain.service.GestaoReservas;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultarPlanoUseCase {

    private final PlanoDeVooRepositoryPort planos;
    private final GestaoReservas reservas;

    public ConsultarPlanoUseCase(PlanoDeVooRepositoryPort planos, GestaoReservas reservas) {
        this.planos = planos;
        this.reservas = reservas;
    }

    @Transactional(readOnly = true)
    public PlanoResponse executar(Long planoId) {
        return planos.buscarPorId(planoId)
                .map(p -> PlanoResponse.de(p, reservas.doPlano(planoId)))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Plano " + planoId + " não encontrado."));
    }
}
