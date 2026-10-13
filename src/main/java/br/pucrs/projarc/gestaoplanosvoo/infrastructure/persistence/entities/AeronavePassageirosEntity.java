package br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("PASSAGEIROS")
public class AeronavePassageirosEntity extends AeronaveEntity {

    @Column(name = "companhia_aerea")
    private String companhiaAerea;

    @Column(name = "capacidade_passageiros")
    private Integer capacidadePassageiros;

    protected AeronavePassageirosEntity() {
    }

    public String getCompanhiaAerea() { return companhiaAerea; }
    public Integer getCapacidadePassageiros() { return capacidadePassageiros; }
}
