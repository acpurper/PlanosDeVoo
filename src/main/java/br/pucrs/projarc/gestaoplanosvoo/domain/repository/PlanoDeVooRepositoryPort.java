package br.pucrs.projarc.gestaoplanosvoo.domain.repository;

import br.pucrs.projarc.gestaoplanosvoo.domain.model.PlanoDeVoo;

import java.util.Optional;

public interface PlanoDeVooRepositoryPort {

    PlanoDeVoo salvar(PlanoDeVoo plano);

    Optional<PlanoDeVoo> buscarPorId(Long id);

    Optional<PlanoDeVoo> buscarPorIdParaAtualizacao(Long id);

    boolean existeAlgum();
}
