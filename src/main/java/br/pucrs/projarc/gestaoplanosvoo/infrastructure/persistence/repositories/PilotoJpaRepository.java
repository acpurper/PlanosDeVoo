package br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.repositories;

import br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.entities.PilotoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PilotoJpaRepository extends JpaRepository<PilotoEntity, Long> {
}
