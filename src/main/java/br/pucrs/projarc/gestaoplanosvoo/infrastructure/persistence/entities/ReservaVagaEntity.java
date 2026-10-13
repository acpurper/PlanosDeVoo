package br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Uma linha por (vaga, slot); a restrição UNIQUE do banco impede reserva dupla. */
@Entity
@Table(name = "reserva_vaga")
public class ReservaVagaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plano_id")
    private Long planoId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "vaga_id")
    private VagaEntity vaga;

    @Column(name = "slot_inicio")
    private LocalDateTime slotInicio;

    protected ReservaVagaEntity() {
    }

    public ReservaVagaEntity(Long planoId, VagaEntity vaga, LocalDateTime slotInicio) {
        this.planoId = planoId;
        this.vaga = vaga;
        this.slotInicio = slotInicio;
    }

    public Long getId() { return id; }
    public Long getPlanoId() { return planoId; }
    public VagaEntity getVaga() { return vaga; }
    public LocalDateTime getSlotInicio() { return slotInicio; }
}
