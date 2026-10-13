package br.pucrs.projarc.gestaoplanosvoo.domain.repository;

import br.pucrs.projarc.gestaoplanosvoo.domain.model.PlanoDeVoo;

import java.util.Optional;

public interface PlanoDeVooRepositoryPort {

    PlanoDeVoo salvar(PlanoDeVoo plano);

    Optional<PlanoDeVoo> buscarPorId(Long id);

    /** Busca bloqueando o plano até o fim da transação (aprovação e cancelamento). */
    Optional<PlanoDeVoo> buscarPorIdParaAtualizacao(Long id);

    boolean existeAlgum();
}
