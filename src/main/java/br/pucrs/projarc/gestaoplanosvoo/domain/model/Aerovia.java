package br.pucrs.projarc.gestaoplanosvoo.domain.model;

import br.pucrs.projarc.gestaoplanosvoo.domain.exception.RegraDeNegocioException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Objects;

/** Ligação orientada origem → destino. Imutável: não há métodos de alteração (enunciado, seção 2). */
public final class Aerovia {

    private final String id;
    private final Aeroporto origem;
    private final Aeroporto destino;
    private final double distanciaKm;

    public Aerovia(String id, Aeroporto origem, Aeroporto destino, double distanciaKm) {
        this.id = Objects.requireNonNull(id);
        this.origem = Objects.requireNonNull(origem);
        this.destino = Objects.requireNonNull(destino);
        if (origem.equals(destino)) {
            throw new RegraDeNegocioException("AEROVIA_INVALIDA", "Aerovia " + id + " com origem igual ao destino.");
        }
        if (distanciaKm <= 0) {
            throw new RegraDeNegocioException("AEROVIA_INVALIDA", "Aerovia " + id + " com distância não positiva.");
        }
        this.distanciaKm = distanciaKm;
    }

    public String getId() { return id; }
    public Aeroporto getOrigem() { return origem; }
    public Aeroporto getDestino() { return destino; }
    public double getDistanciaKm() { return distanciaKm; }

    /**
     * Tempo para percorrer a aerovia: distância / velocidade, em minutos, arredondado para cima.
     * Usa BigDecimal para que contas exatas (ex.: 360 km a 600 km/h = 36 min) não virem 37 por erro de ponto flutuante.
     */
    public Duration duracao(double velocidadeKmh) {
        long minutos = BigDecimal.valueOf(distanciaKm)
                .multiply(BigDecimal.valueOf(60))
                .divide(BigDecimal.valueOf(velocidadeKmh), 0, RoundingMode.CEILING)
                .longValueExact();
        return Duration.ofMinutes(minutos);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Aerovia a && a.id.equals(id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id + " (" + origem + "→" + destino + ")";
    }
}
