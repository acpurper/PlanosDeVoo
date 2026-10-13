package br.pucrs.projarc.gestaoplanosvoo.domain.model.aeronave;

import java.util.List;

public final class AeronavePassageiros extends Aeronave {

    public static final String CATEGORIA = "PASSAGEIROS";
    private static final List<RegraOperacao> REGRAS = List.of(new AltitudeMinima(28_000));

    private final String companhiaAerea;
    private final int capacidadePassageiros;

    public AeronavePassageiros(String prefixo, double velocidadeCruzeiroKmh, double autonomiaKm,
                               String companhiaAerea, int capacidadePassageiros) {
        super(prefixo, velocidadeCruzeiroKmh, autonomiaKm);
        this.companhiaAerea = companhiaAerea;
        this.capacidadePassageiros = capacidadePassageiros;
    }

    public String getCompanhiaAerea() { return companhiaAerea; }
    public int getCapacidadePassageiros() { return capacidadePassageiros; }

    @Override
    public String categoria() { return CATEGORIA; }

    @Override
    public List<RegraOperacao> regras() { return REGRAS; }
}
