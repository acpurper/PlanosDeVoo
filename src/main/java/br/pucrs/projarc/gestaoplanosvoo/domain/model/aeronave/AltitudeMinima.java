package br.pucrs.projarc.gestaoplanosvoo.domain.model.aeronave;

import br.pucrs.projarc.gestaoplanosvoo.domain.model.Altitude;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.CodigoViolacao;

import java.time.LocalDateTime;

public final class AltitudeMinima implements RegraOperacao {

    private final int minimaPes;

    public AltitudeMinima(int minimaPes) {
        this.minimaPes = new Altitude(minimaPes).pes();
    }

    @Override
    public boolean permite(Altitude altitude, LocalDateTime ini, LocalDateTime fim) {
        return altitude.pes() >= minimaPes;
    }

    @Override
    public CodigoViolacao codigoViolacao() {
        return CodigoViolacao.ALTITUDE_PROIBIDA_CATEGORIA;
    }

    @Override
    public String descricao() {
        return "altitude mínima de " + minimaPes + " pés";
    }
}
