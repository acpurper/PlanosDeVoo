package br.pucrs.projarc.gestaoplanosvoo.application.usecases;

import br.pucrs.projarc.gestaoplanosvoo.application.dto.response.PlanoResponse;
import br.pucrs.projarc.gestaoplanosvoo.domain.exception.RecursoNaoEncontradoException;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.PlanoDeVoo;
import br.pucrs.projarc.gestaoplanosvoo.domain.repository.PlanoDeVooRepositoryPort;
import br.pucrs.projarc.gestaoplanosvoo.domain.service.GestaoReservas;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC07: aprova o plano e reserva os recursos.
 *
 * <p>O serviço de reservas é recebido por injeção de dependência no construtor, pela interface
 * {@link GestaoReservas}. O caso de uso não depende de outro caso de uso nem do repositório de reservas.
 */
@Service
public class AprovarPlanoUseCase {

    private final PlanoDeVooRepositoryPort planos;
    private final GestaoReservas reservas;

    public AprovarPlanoUseCase(PlanoDeVooRepositoryPort planos, GestaoReservas reservas) {
        this.planos = planos;
        this.reservas = reservas;
    }

    @Transactional
    public PlanoResponse executar(Long planoId) {
        PlanoDeVoo plano = planos.buscarPorIdParaAtualizacao(planoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Plano " + planoId + " não encontrado."));

        // Revalida com a ocupação do momento; lança PlanoInvalidoException (422) se houver violações.
        plano.aprovar(reservas);
        planos.salvar(plano);
        reservas.reservar(plano);

        return PlanoResponse.de(planos.buscarPorId(planoId).orElseThrow(), reservas.doPlano(planoId));
    }
}
