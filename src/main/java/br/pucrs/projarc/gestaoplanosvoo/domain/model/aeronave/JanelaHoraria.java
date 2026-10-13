package br.pucrs.projarc.gestaoplanosvoo.domain.model.aeronave;

import br.pucrs.projarc.gestaoplanosvoo.domain.model.Altitude;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.CodigoViolacao;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Todo o uso [ini, fim) precisa caber na janela [inicio, fim) do dia em que começa
 * (ex.: carga, 00:00 inclusive a 06:00 exclusive). Terminar exatamente às 06:00 é permitido.
 */
public final class JanelaHoraria implements RegraOperacao {

    private final LocalTime inicio;
    private final LocalTime fim;

    public JanelaHoraria(LocalTime inicio, LocalTime fim) {
        if (!inicio.isBefore(fim)) {
            throw new IllegalArgumentException("Janela inválida: " + inicio + " a " + fim);
        }
        this.inicio = inicio;
        this.fim = fim;
    }

    @Override
    public boolean permite(Altitude altitude, LocalDateTime iniUso, LocalDateTime fimUso) {
        LocalDateTime aberturaJanela = iniUso.toLocalDate().atTime(inicio);
        LocalDateTime fechamentoJanela = iniUso.toLocalDate().atTime(fim);
        return !iniUso.isBefore(aberturaJanela) && !fimUso.isAfter(fechamentoJanela);
    }

    @Override
    public CodigoViolacao codigoViolacao() {
        return CodigoViolacao.HORARIO_PROIBIDO_CATEGORIA;
    }

    @Override
    public String descricao() {
        return "operação somente entre " + inicio + " (inclusive) e " + fim + " (exclusive)";
    }
}
