package br.pucrs.projarc.gestaoplanosvoo.application.usecases;

import br.pucrs.projarc.gestaoplanosvoo.application.dto.response.PlanoResponse;
import br.pucrs.projarc.gestaoplanosvoo.domain.exception.PlanoInvalidoException;
import br.pucrs.projarc.gestaoplanosvoo.domain.exception.RecursoNaoEncontradoException;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.PlanoDeVoo;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.ResultadoValidacao;
import br.pucrs.projarc.gestaoplanosvoo.domain.repository.PlanoDeVooRepositoryPort;
import br.pucrs.projarc.gestaoplanosvoo.domain.service.ReservaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AprovarPlanoUseCase {

    private final PlanoDeVooRepositoryPort planos;
    private final ReservaService reservaService;
    private final ValidarPlanoUseCase validarPlano;

    public AprovarPlanoUseCase(PlanoDeVooRepositoryPort planos, ReservaService reservaService,
                               ValidarPlanoUseCase validarPlano) {
        this.planos = planos;
        this.reservaService = reservaService;
        this.validarPlano = validarPlano;
    }

    @Transactional
    public PlanoResponse executar(Long planoId) {
        PlanoDeVoo plano = planos.buscarPorIdParaAtualizacao(planoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Plano " + planoId + " não encontrado."));

        ResultadoValidacao resultado = validarPlano.validar(plano);
        if (!resultado.valido()) {
            throw new PlanoInvalidoException(resultado);
        }

        plano.aprovar(reservaService);
        planos.salvar(plano);
        reservaService.reservar(plano);

        return PlanoResponse.de(planos.buscarPorId(planoId).orElseThrow(), reservaService.doPlano(planoId));
    }
}
