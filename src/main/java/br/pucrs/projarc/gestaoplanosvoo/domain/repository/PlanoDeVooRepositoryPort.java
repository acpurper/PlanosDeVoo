package br.pucrs.projarc.gestaoplanosvoo.domain.repository;

import br.pucrs.projarc.gestaoplanosvoo.domain.model.PlanoDeVoo;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.SituacaoPlano;

import java.util.List;
import java.util.Optional;

public interface PlanoDeVooRepositoryPort {

    PlanoDeVoo salvar(PlanoDeVoo plano);

    Optional<PlanoDeVoo> buscarPorId(Long id);

    Optional<PlanoDeVoo> buscarPorIdParaAtualizacao(Long id);

    /** Todos os planos, em ordem de identificador. */
    List<PlanoDeVoo> listarTodos();

    List<PlanoDeVoo> listarPorSituacao(SituacaoPlano situacao);

    boolean existeAlgum();
}
