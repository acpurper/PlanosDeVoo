package br.pucrs.projarc.gestaoplanosvoo.domain.model;

import java.util.Objects;

/** Posição de estacionamento de um aeroporto. Identificada pelo par (aeroporto, número). */
public final class Vaga {

    private final Long id;
    private final String aeroportoCodigo;
    private final int numero;

    public Vaga(Long id, String aeroportoCodigo, int numero) {
        this.id = id;
        this.aeroportoCodigo = Objects.requireNonNull(aeroportoCodigo);
        this.numero = numero;
    }

    public Long getId() { return id; }
    public String getAeroportoCodigo() { return aeroportoCodigo; }
    public int getNumero() { return numero; }

    public boolean pertenceA(Aeroporto aeroporto) {
        return aeroportoCodigo.equals(aeroporto.getCodigo());
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Vaga v && v.numero == numero && v.aeroportoCodigo.equals(aeroportoCodigo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aeroportoCodigo, numero);
    }

    @Override
    public String toString() {
        return aeroportoCodigo + "-" + numero;
    }
}
