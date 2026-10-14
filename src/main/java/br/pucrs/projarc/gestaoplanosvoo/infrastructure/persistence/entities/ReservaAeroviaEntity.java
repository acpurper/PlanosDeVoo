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

/** Uma linha por (aerovia, altitude, slot); a restrição UNIQUE do banco impede reserva dupla. */
@Entity
@Table(name = "reserva_aerovia")
public class ReservaAeroviaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plano_id")
    private Long planoId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "aerovia_id")
    private AeroviaEntity aerovia;

    @Column(name = "altitude_pes")
    private int altitudePes;

    @Column(name = "slot_inicio")
    private LocalDateTime slotInicio;

    protected ReservaAeroviaEntity() {
    }

    public ReservaAeroviaEntity(Long planoId, AeroviaEntity aerovia, int altitudePes, LocalDateTime slotInicio) {
        this.planoId = planoId;
        this.aerovia = aerovia;
        this.altitudePes = altitudePes;
        this.slotInicio = slotInicio;
    }

    public Long getId() { return id; }
    public Long getPlanoId() { return planoId; }
    public AeroviaEntity getAerovia() { return aerovia; }
    public int getAltitudePes() { return altitudePes; }
    public LocalDateTime getSlotInicio() { return slotInicio; }
}
