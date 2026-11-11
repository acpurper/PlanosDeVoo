package br.pucrs.projarc.gestaoplanosvoo.application.usecases;

import br.pucrs.projarc.gestaoplanosvoo.application.dto.response.PlanoResponse;
import br.pucrs.projarc.gestaoplanosvoo.domain.exception.RecursoNaoEncontradoException;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.PlanoDeVoo;
import br.pucrs.projarc.gestaoplanosvoo.domain.repository.PlanoDeVooRepositoryPort;
import br.pucrs.projarc.gestaoplanosvoo.domain.service.ReservaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class CancelarPlanoUseCase {

    private final PlanoDeVooRepositoryPort planos;
    private final ReservaService reservaService;
    private final Clock clock;

    public CancelarPlanoUseCase(PlanoDeVooRepositoryPort planos, ReservaService reservaService, Clock clock) {
        this.planos = planos;
        this.reservaService = reservaService;
        this.clock = clock;
    }

    @Transactional
    public PlanoResponse executar(Long planoId) {
        PlanoDeVoo plano = planos.buscarPorIdParaAtualizacao(planoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Plano " + planoId + " não encontrado."));
        plano.cancelar(LocalDateTime.now(clock));
        reservaService.liberar(planoId);
        return PlanoResponse.de(planos.salvar(plano), reservaService.doPlano(planoId));
    }
}
