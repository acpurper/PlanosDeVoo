package br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "aeroporto")
public class AeroportoEntity {

    @Id
    private String codigo;

    private String nome;

    @OneToMany(mappedBy = "aeroportoCodigo")
    @OrderBy("numero")
    private List<VagaEntity> vagas = new ArrayList<>();

    protected AeroportoEntity() {
    }

    public String getCodigo() { return codigo; }
    public String getNome() { return nome; }
    public List<VagaEntity> getVagas() { return vagas; }
}
