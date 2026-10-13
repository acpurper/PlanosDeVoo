package br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.repositories;

import br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.entities.AeroviaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AeroviaJpaRepository extends JpaRepository<AeroviaEntity, String> {
}
