package br.pucrs.projarc.gestaoplanosvoo.infrastructure.api;

import br.pucrs.projarc.gestaoplanosvoo.application.dto.response.AeronaveDetalheResponse;
import br.pucrs.projarc.gestaoplanosvoo.application.dto.response.AeroportoResponse;
import br.pucrs.projarc.gestaoplanosvoo.application.dto.response.AeroviaResponse;
import br.pucrs.projarc.gestaoplanosvoo.application.dto.response.DisponibilidadeAeroviaResponse;
import br.pucrs.projarc.gestaoplanosvoo.application.dto.response.DisponibilidadeVagasResponse;
import br.pucrs.projarc.gestaoplanosvoo.application.dto.response.PilotoResponse;
import br.pucrs.projarc.gestaoplanosvoo.application.usecases.ConsultarAltitudesLivresUseCase;
import br.pucrs.projarc.gestaoplanosvoo.application.usecases.ConsultarMalhaUseCase;
import br.pucrs.projarc.gestaoplanosvoo.application.usecases.ConsultarVagasLivresUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/** Consultas da malha, dos recursos e da disponibilidade (UC01, UC04 e UC05). Somente leitura. */
@RestController
@Tag(name = "Malha, recursos e disponibilidade")
public class MalhaRecursosController {

    private final ConsultarMalhaUseCase consultarMalha;
    private final ConsultarAltitudesLivresUseCase altitudesLivres;
    private final ConsultarVagasLivresUseCase vagasLivres;

    public MalhaRecursosController(ConsultarMalhaUseCase consultarMalha, ConsultarAltitudesLivresUseCase altitudesLivres,
                                   ConsultarVagasLivresUseCase vagasLivres) {
        this.consultarMalha = consultarMalha;
        this.altitudesLivres = altitudesLivres;
        this.vagasLivres = vagasLivres;
    }

    @GetMapping("/aeroportos")
    @Operation(summary = "Listar aeroportos")
    public List<AeroportoResponse> aeroportos() {
        return consultarMalha.aeroportos();
    }

    @GetMapping("/aeroportos/{codigo}")
    @Operation(summary = "Consultar aeroporto")
    @ApiResponse(responseCode = "200", description = "Aeroporto encontrado")
    @ApiResponse(responseCode = "404", description = "Aeroporto inexistente")
    public AeroportoResponse aeroporto(@PathVariable String codigo) {
        return consultarMalha.aeroporto(codigo);
    }

    @GetMapping("/aeroportos/{codigo}/vagas-livres")
    @Operation(summary = "Consultar vagas livres de um aeroporto",
            description = "Vagas livres em todos os slots de uma hora que o período toca. Não reserva nada.")
    @ApiResponse(responseCode = "200", description = "Vagas livres e ocupadas no período")
    @ApiResponse(responseCode = "400", description = "Parâmetro ausente ou data mal formatada")
    @ApiResponse(responseCode = "404", description = "Aeroporto inexistente")
    @ApiResponse(responseCode = "422", description = "Fim do período não é posterior ao início")
    public DisponibilidadeVagasResponse vagasLivres(
            @PathVariable String codigo,
            @Parameter(example = "2026-12-01T09:00") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @Parameter(example = "2026-12-01T11:00") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim) {
        return vagasLivres.executar(codigo, inicio, fim);
    }

    @GetMapping("/aerovias")
    @Operation(summary = "Listar aerovias")
    public List<AeroviaResponse> aerovias() {
        return consultarMalha.aerovias();
    }

    @GetMapping("/aerovias/{id}")
    @Operation(summary = "Consultar aerovia")
    @ApiResponse(responseCode = "200", description = "Aerovia encontrada")
    @ApiResponse(responseCode = "404", description = "Aerovia inexistente")
    public AeroviaResponse aerovia(@PathVariable String id) {
        return consultarMalha.aerovia(id);
    }

    @GetMapping("/aerovias/{id}/altitudes-livres")
    @Operation(summary = "Consultar altitudes livres de uma aerovia",
            description = "Altitudes livres em todos os slots de uma hora que o período toca. Não reserva nada.")
    @ApiResponse(responseCode = "200", description = "Altitudes livres e ocupadas no período")
    @ApiResponse(responseCode = "400", description = "Parâmetro ausente ou data mal formatada")
    @ApiResponse(responseCode = "404", description = "Aerovia inexistente")
    @ApiResponse(responseCode = "422", description = "Fim do período não é posterior ao início")
    public DisponibilidadeAeroviaResponse altitudesLivres(
            @PathVariable String id,
            @Parameter(example = "2026-12-01T08:00") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @Parameter(example = "2026-12-01T10:00") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim) {
        return altitudesLivres.executar(id, inicio, fim);
    }

    @GetMapping("/pilotos")
    @Operation(summary = "Listar pilotos")
    public List<PilotoResponse> pilotos() {
        return consultarMalha.pilotos();
    }

    @GetMapping("/aeronaves")
    @Operation(summary = "Listar aeronaves", description = "Inclui os dados da categoria e as regras de operação.")
    public List<AeronaveDetalheResponse> aeronaves() {
        return consultarMalha.aeronaves();
    }

    @GetMapping("/aeronaves/{prefixo}")
    @Operation(summary = "Consultar aeronave")
    @ApiResponse(responseCode = "200", description = "Aeronave encontrada")
    @ApiResponse(responseCode = "404", description = "Aeronave inexistente")
    public AeronaveDetalheResponse aeronave(@PathVariable String prefixo) {
        return consultarMalha.aeronave(prefixo);
    }
}
