package br.pucrs.projarc.gestaoplanosvoo.domain.model.aeronave;

import br.pucrs.projarc.gestaoplanosvoo.domain.model.Altitude;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.CodigoViolacao;

import java.time.LocalDateTime;

public interface RegraOperacao {

    boolean permite(Altitude altitude, LocalDateTime ini, LocalDateTime fim);

    CodigoViolacao codigoViolacao();

    String descricao();
}
