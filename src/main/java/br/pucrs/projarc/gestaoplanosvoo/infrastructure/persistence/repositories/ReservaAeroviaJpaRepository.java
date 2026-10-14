package br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.repositories;

import br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.entities.ReservaAeroviaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservaAeroviaJpaRepository extends JpaRepository<ReservaAeroviaEntity, Long> {

    boolean existsByAerovia_IdAndAltitudePesAndSlotInicio(String aeroviaId, int altitudePes, LocalDateTime slotInicio);

    List<ReservaAeroviaEntity> findByPlanoIdOrderBySlotInicio(Long planoId);

    @Modifying(flushAutomatically = true)
    @Query("delete from ReservaAeroviaEntity r where r.planoId = :planoId")
    int apagarDoPlano(Long planoId);
}
