package br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.repositories;

import br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.entities.ReservaVagaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservaVagaJpaRepository extends JpaRepository<ReservaVagaEntity, Long> {

    boolean existsByVaga_IdAndSlotInicio(Long vagaId, LocalDateTime slotInicio);

    List<ReservaVagaEntity> findByPlanoIdOrderBySlotInicio(Long planoId);

    @Modifying(flushAutomatically = true)
    @Query("delete from ReservaVagaEntity r where r.planoId = :planoId")
    int apagarDoPlano(Long planoId);
}
