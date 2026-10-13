package br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("PARTICULAR")
public class AeronaveParticularEntity extends AeronaveEntity {

    @Column(name = "empresa_manutencao")
    private String empresaManutencao;

    protected AeronaveParticularEntity() {
    }

    public String getEmpresaManutencao() { return empresaManutencao; }
}
