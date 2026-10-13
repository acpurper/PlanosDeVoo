package br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.repositories;

import br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.entities.VagaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VagaJpaRepository extends JpaRepository<VagaEntity, Long> {
}
