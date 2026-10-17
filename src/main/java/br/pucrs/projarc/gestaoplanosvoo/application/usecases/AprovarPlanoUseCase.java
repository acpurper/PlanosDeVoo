package br.pucrs.projarc.gestaoplanosvoo.application.usecases;

import br.pucrs.projarc.gestaoplanosvoo.application.dto.response.PlanoResponse;
import br.pucrs.projarc.gestaoplanosvoo.domain.exception.PlanoInvalidoException;
import br.pucrs.projarc.gestaoplanosvoo.domain.exception.RecursoNaoEncontradoException;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.PlanoDeVoo;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.ResultadoValidacao;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.reserva.Reserva;
import br.pucrs.projarc.gestaoplanosvoo.domain.repository.PlanoDeVooRepositoryPort;
import br.pucrs.projarc.gestaoplanosvoo.domain.repository.ReservaRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AprovarPlanoUseCase {

    private final PlanoDeVooRepositoryPort planos;
    private final ReservaRepositoryPort reservas;
    private final ValidarPlanoUseCase validarPlano;

    public AprovarPlanoUseCase(PlanoDeVooRepositoryPort planos, ReservaRepositoryPort reservas,
                               ValidarPlanoUseCase validarPlano) {
        this.planos = planos;
        this.reservas = reservas;
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

        List<Reserva> novas = plano.aprovar(reservas);
        planos.salvar(plano);
        reservas.salvarTodas(novas);

        return PlanoResponse.de(planos.buscarPorId(planoId).orElseThrow());
    }
}
