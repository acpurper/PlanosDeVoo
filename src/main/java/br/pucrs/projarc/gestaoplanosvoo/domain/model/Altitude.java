package br.pucrs.projarc.gestaoplanosvoo.domain.model;

import br.pucrs.projarc.gestaoplanosvoo.domain.exception.RegraDeNegocioException;

import java.util.List;
import java.util.stream.IntStream;

/** Nível de voo: 25.000 a 35.000 pés, inclusive, em passos de 1.000 (enunciado, 5.1). */
public record Altitude(int pes) {

    public static final int MINIMA = 25_000;
    public static final int MAXIMA = 35_000;
    public static final int PASSO = 1_000;

    public Altitude {
        if (pes < MINIMA || pes > MAXIMA || pes % PASSO != 0) {
            throw new RegraDeNegocioException("ALTITUDE_INVALIDA",
                    "Altitude " + pes + " inválida: use de " + MINIMA + " a " + MAXIMA + " pés, em passos de " + PASSO + ".");
        }
    }

    public static Altitude de(int pes) {
        return new Altitude(pes);
    }

    public static List<Altitude> todas() {
        return IntStream.rangeClosed(MINIMA / PASSO, MAXIMA / PASSO).mapToObj(n -> new Altitude(n * PASSO)).toList();
    }
}
