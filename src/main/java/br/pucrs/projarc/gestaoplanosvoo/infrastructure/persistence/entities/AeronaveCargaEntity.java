package br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("CARGA")
public class AeronaveCargaEntity extends AeronaveEntity {

    @Column(name = "companhia_aerea")
    private String companhiaAerea;

    @Column(name = "capacidade_toneladas")
    private Double capacidadeToneladas;

    protected AeronaveCargaEntity() {
    }

    public String getCompanhiaAerea() { return companhiaAerea; }
    public Double getCapacidadeToneladas() { return capacidadeToneladas; }
}
