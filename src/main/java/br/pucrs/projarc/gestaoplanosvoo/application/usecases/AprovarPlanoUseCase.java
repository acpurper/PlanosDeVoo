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

/**
 * UC07 — Aprovar plano. Numa única transação: bloqueia o plano, revalida com a disponibilidade do momento
 * (mesma lógica do UC06), grava uma reserva por slot e passa a APROVADO. Qualquer falha desfaz tudo.
 */
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

        // «include» UC06: violações -> 422 e o plano continua EM_EDICAO (nada foi gravado).
        ResultadoValidacao resultado = validarPlano.validar(plano);
        if (!resultado.valido()) {
            throw new PlanoInvalidoException(resultado);
        }

        List<Reserva> novas = plano.aprovar(reservas);
        planos.salvar(plano);
        // INSERT + flush: se outro plano levou um slot nesse meio-tempo, a UNIQUE falha,
        // ConflitoReservaException sobe e a transação inteira é desfeita (409).
        reservas.salvarTodas(novas);

        return PlanoResponse.de(planos.buscarPorId(planoId).orElseThrow());
    }
}
