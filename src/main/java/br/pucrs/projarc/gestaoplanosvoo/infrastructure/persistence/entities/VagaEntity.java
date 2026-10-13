package br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "vaga")
public class VagaEntity {

    @Id
    private Long id;

    @Column(name = "aeroporto_codigo")
    private String aeroportoCodigo;

    private int numero;

    protected VagaEntity() {
    }

    public Long getId() { return id; }
    public String getAeroportoCodigo() { return aeroportoCodigo; }
    public int getNumero() { return numero; }
}
