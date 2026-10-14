package br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.repositories;

import br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.entities.AeronaveEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AeronaveJpaRepository extends JpaRepository<AeronaveEntity, String> {
}
