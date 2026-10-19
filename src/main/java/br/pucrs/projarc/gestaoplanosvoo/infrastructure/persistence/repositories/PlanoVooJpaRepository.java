package br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.repositories;

import br.pucrs.projarc.gestaoplanosvoo.domain.model.SituacaoPlano;
import br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.entities.PlanoVooEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PlanoVooJpaRepository extends JpaRepository<PlanoVooEntity, Long> {

    /** SELECT ... FOR UPDATE: serializa aprovação e cancelamento do mesmo plano. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PlanoVooEntity p where p.id = :id")
    Optional<PlanoVooEntity> buscarComBloqueio(Long id);

    List<PlanoVooEntity> findAllByOrderById();

    List<PlanoVooEntity> findBySituacaoOrderById(SituacaoPlano situacao);
}
