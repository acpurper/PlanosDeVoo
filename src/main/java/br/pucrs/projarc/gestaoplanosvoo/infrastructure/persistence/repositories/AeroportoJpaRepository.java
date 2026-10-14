package br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.repositories;

import br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.entities.AeroportoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AeroportoJpaRepository extends JpaRepository<AeroportoEntity, String> {
}
