package br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "piloto")
public class PilotoEntity {

    @Id
    private Long id;

    private String nome;

    protected PilotoEntity() {
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
}
