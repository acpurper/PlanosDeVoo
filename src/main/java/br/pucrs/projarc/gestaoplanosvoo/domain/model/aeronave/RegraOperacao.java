package br.pucrs.projarc.gestaoplanosvoo.domain.model.aeronave;

import br.pucrs.projarc.gestaoplanosvoo.domain.model.Altitude;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.CodigoViolacao;

import java.time.LocalDateTime;

/**
 * Strategy: diz se uma aeronave pode usar uma altitude durante o intervalo [ini, fim).
 * Uma regra nova é uma classe nova que implementa esta interface; nada mais precisa mudar.
 */
public interface RegraOperacao {

    boolean permite(Altitude altitude, LocalDateTime ini, LocalDateTime fim);

    /** Código da violação gerada quando a regra não é atendida. */
    CodigoViolacao codigoViolacao();

    /** Descrição legível da regra, usada nas mensagens. */
    String descricao();
}
