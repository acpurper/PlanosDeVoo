package br.pucrs.projarc.gestaoplanosvoo.application.usecases;

import br.pucrs.projarc.gestaoplanosvoo.application.dto.response.PlanoResponse;
import br.pucrs.projarc.gestaoplanosvoo.domain.exception.RecursoNaoEncontradoException;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.PlanoDeVoo;
import br.pucrs.projarc.gestaoplanosvoo.domain.repository.PlanoDeVooRepositoryPort;
import br.pucrs.projarc.gestaoplanosvoo.domain.repository.ReservaRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class CancelarPlanoUseCase {

    private final PlanoDeVooRepositoryPort planos;
    private final ReservaRepositoryPort reservas;
    private final Clock clock;

    public CancelarPlanoUseCase(PlanoDeVooRepositoryPort planos, ReservaRepositoryPort reservas, Clock clock) {
        this.planos = planos;
        this.reservas = reservas;
        this.clock = clock;
    }

    @Transactional
    public PlanoResponse executar(Long planoId) {
        PlanoDeVoo plano = planos.buscarPorIdParaAtualizacao(planoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Plano " + planoId + " não encontrado."));
        plano.cancelar(LocalDateTime.now(clock));
        reservas.removerDoPlano(planoId);
        return PlanoResponse.de(planos.salvar(plano));
    }
}
