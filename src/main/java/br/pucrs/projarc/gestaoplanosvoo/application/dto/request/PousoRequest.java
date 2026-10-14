package br.pucrs.projarc.gestaoplanosvoo.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Pouso no aeroporto de destino da aerovia. Ausente = sobrevoo.")
public record PousoRequest(
        @Schema(description = "Número da vaga no aeroporto de destino da aerovia", example = "2") @NotNull Integer vaga,
        @Schema(description = "Tempo de permanência em minutos", example = "30") @NotNull @Positive Integer permanenciaMin) {
}
