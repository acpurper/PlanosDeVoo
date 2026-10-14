package br.pucrs.projarc.gestaoplanosvoo.domain.model;

import java.util.Objects;

/** Responsável pelo plano. Sem regras próprias (enunciado, seção 4). */
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
