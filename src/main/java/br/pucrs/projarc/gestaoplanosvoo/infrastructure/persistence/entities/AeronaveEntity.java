package br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

/** Hierarquia de aeronaves numa tabela só, discriminada pela coluna categoria. */
@Entity
@Table(name = "aeronave")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "categoria")
public abstract class AeronaveEntity {

    @Id
    private String prefixo;

    @Column(name = "velocidade_cruzeiro_kmh")
    private double velocidadeCruzeiroKmh;

    @Column(name = "autonomia_km")
    private double autonomiaKm;

    protected AeronaveEntity() {
    }

    public String getPrefixo() { return prefixo; }
    public double getVelocidadeCruzeiroKmh() { return velocidadeCruzeiroKmh; }
    public double getAutonomiaKm() { return autonomiaKm; }
}
