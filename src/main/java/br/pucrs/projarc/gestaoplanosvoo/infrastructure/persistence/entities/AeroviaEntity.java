package br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/** Aerovia é imutável: o Hibernate nunca emite UPDATE para ela. */
@Entity
@Immutable
@Table(name = "aerovia")
public class AeroviaEntity {

    @Id
    private String id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "origem")
    private AeroportoEntity origem;

    @ManyToOne(optional = false)
    @JoinColumn(name = "destino")
    private AeroportoEntity destino;

    @Column(name = "distancia_km")
    private double distanciaKm;

    protected AeroviaEntity() {
    }

    public String getId() { return id; }
    public AeroportoEntity getOrigem() { return origem; }
    public AeroportoEntity getDestino() { return destino; }
    public double getDistanciaKm() { return distanciaKm; }
}
