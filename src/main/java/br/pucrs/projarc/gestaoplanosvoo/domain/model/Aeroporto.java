package br.pucrs.projarc.gestaoplanosvoo.domain.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class Aeroporto {

    private final String codigo;
    private final String nome;
    private final List<Vaga> vagas;

    public Aeroporto(String codigo, String nome, List<Vaga> vagas) {
        this.codigo = Objects.requireNonNull(codigo);
        this.nome = Objects.requireNonNull(nome);
        this.vagas = List.copyOf(vagas);
    }

    public String getCodigo() { return codigo; }
    public String getNome() { return nome; }
    public List<Vaga> getVagas() { return vagas; }

    public Optional<Vaga> vaga(int numero) {
        return vagas.stream().filter(v -> v.getNumero() == numero).findFirst();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Aeroporto a && a.codigo.equals(codigo);
    }

    @Override
    public int hashCode() {
        return codigo.hashCode();
    }

    @Override
    public String toString() {
        return codigo;
    }
}
