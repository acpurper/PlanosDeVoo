package br.pucrs.projarc.gestaoplanosvoo.domain.model.aeronave;

import br.pucrs.projarc.gestaoplanosvoo.domain.exception.RegraDeNegocioException;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Altitude;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Aeronave com dados comuns. Cada subclasse (categoria) declara suas regras de operação;
 * a aeronave só delega para elas (Strategy), sem if/switch por categoria.
 */
public abstract class Aeronave {

    /** Margem de segurança de 15%: só 85% da autonomia nominal pode ser usada sem reabastecer. */
    public static final double FATOR_AUTONOMIA_SEGURA = 0.85;

    private final String prefixo;
    private final double velocidadeCruzeiroKmh;
    private final double autonomiaKm;

    protected Aeronave(String prefixo, double velocidadeCruzeiroKmh, double autonomiaKm) {
        this.prefixo = Objects.requireNonNull(prefixo);
        if (velocidadeCruzeiroKmh <= 0 || autonomiaKm <= 0) {
            throw new RegraDeNegocioException("AERONAVE_INVALIDA", "Velocidade e autonomia devem ser positivas.");
        }
        this.velocidadeCruzeiroKmh = velocidadeCruzeiroKmh;
        this.autonomiaKm = autonomiaKm;
    }

    public String getPrefixo() { return prefixo; }
    public double getVelocidadeCruzeiroKmh() { return velocidadeCruzeiroKmh; }
    public double getAutonomiaKm() { return autonomiaKm; }

    /** Nome da categoria, usado na persistência e nas respostas. */
    public abstract String categoria();

    /** Regras de operação da categoria. */
    public abstract List<RegraOperacao> regras();

    public double autonomiaSegura() {
        return autonomiaKm * FATOR_AUTONOMIA_SEGURA;
    }

    public List<RegraOperacao> regrasVioladas(Altitude altitude, LocalDateTime ini, LocalDateTime fim) {
        return regras().stream().filter(r -> !r.permite(altitude, ini, fim)).toList();
    }

    public boolean podeOperar(Altitude altitude, LocalDateTime ini, LocalDateTime fim) {
        return regrasVioladas(altitude, ini, fim).isEmpty();
    }
}
