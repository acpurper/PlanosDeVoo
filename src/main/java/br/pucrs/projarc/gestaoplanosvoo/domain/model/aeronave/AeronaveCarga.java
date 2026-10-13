package br.pucrs.projarc.gestaoplanosvoo.domain.model.aeronave;

import java.time.LocalTime;
import java.util.List;

public final class AeronaveCarga extends Aeronave {

    public static final String CATEGORIA = "CARGA";
    private static final List<RegraOperacao> REGRAS = List.of(new JanelaHoraria(LocalTime.MIDNIGHT, LocalTime.of(6, 0)));

    private final String companhiaAerea;
    private final double capacidadeToneladas;

    public AeronaveCarga(String prefixo, double velocidadeCruzeiroKmh, double autonomiaKm,
                         String companhiaAerea, double capacidadeToneladas) {
        super(prefixo, velocidadeCruzeiroKmh, autonomiaKm);
        this.companhiaAerea = companhiaAerea;
        this.capacidadeToneladas = capacidadeToneladas;
    }

    public String getCompanhiaAerea() { return companhiaAerea; }
    public double getCapacidadeToneladas() { return capacidadeToneladas; }

    @Override
    public String categoria() { return CATEGORIA; }

    @Override
    public List<RegraOperacao> regras() { return REGRAS; }
}
