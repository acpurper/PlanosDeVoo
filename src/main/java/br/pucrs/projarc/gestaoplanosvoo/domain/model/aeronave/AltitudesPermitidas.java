package br.pucrs.projarc.gestaoplanosvoo.domain.model.aeronave;

import br.pucrs.projarc.gestaoplanosvoo.domain.model.Altitude;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.CodigoViolacao;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.TreeSet;

public final class AltitudesPermitidas implements RegraOperacao {

    private final Set<Integer> pes;

    public AltitudesPermitidas(int... pes) {
        this.pes = new TreeSet<>();
        for (int p : pes) {
            this.pes.add(new Altitude(p).pes());
        }
    }

    @Override
    public boolean permite(Altitude altitude, LocalDateTime ini, LocalDateTime fim) {
        return pes.contains(altitude.pes());
    }

    @Override
    public CodigoViolacao codigoViolacao() {
        return CodigoViolacao.ALTITUDE_PROIBIDA_CATEGORIA;
    }

    @Override
    public String descricao() {
        return "somente as altitudes " + pes + " pés";
    }
}
