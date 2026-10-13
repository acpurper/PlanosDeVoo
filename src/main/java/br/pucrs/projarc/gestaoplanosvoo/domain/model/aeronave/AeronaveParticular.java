package br.pucrs.projarc.gestaoplanosvoo.domain.model.aeronave;

import java.util.List;

public final class AeronaveParticular extends Aeronave {

    public static final String CATEGORIA = "PARTICULAR";
    private static final List<RegraOperacao> REGRAS = List.of(new AltitudesPermitidas(25_000, 26_000, 27_000));

    private final String empresaManutencao;

    public AeronaveParticular(String prefixo, double velocidadeCruzeiroKmh, double autonomiaKm, String empresaManutencao) {
        super(prefixo, velocidadeCruzeiroKmh, autonomiaKm);
        this.empresaManutencao = empresaManutencao;
    }

    public String getEmpresaManutencao() { return empresaManutencao; }

    @Override
    public String categoria() { return CATEGORIA; }

    @Override
    public List<RegraOperacao> regras() { return REGRAS; }
}
