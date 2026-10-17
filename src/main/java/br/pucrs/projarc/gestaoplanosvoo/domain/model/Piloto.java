package br.pucrs.projarc.gestaoplanosvoo.domain.model;

import java.util.Objects;

public final class Piloto {

    private final Long id;
    private final String nome;

    public Piloto(Long id, String nome) {
        this.id = id;
        this.nome = Objects.requireNonNull(nome);
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
}
