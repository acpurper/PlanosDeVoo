package br.pucrs.projarc.gestaoplanosvoo.infrastructure.api;

import br.pucrs.projarc.gestaoplanosvoo.application.dto.request.CriarPlanoRequest;
import br.pucrs.projarc.gestaoplanosvoo.application.dto.request.TrechoRequest;
import br.pucrs.projarc.gestaoplanosvoo.application.dto.response.PlanoResponse;
import br.pucrs.projarc.gestaoplanosvoo.application.dto.response.PlanoResumoResponse;
import br.pucrs.projarc.gestaoplanosvoo.application.dto.response.ResultadoValidacaoResponse;
import br.pucrs.projarc.gestaoplanosvoo.application.usecases.AbandonarPlanoUseCase;
import br.pucrs.projarc.gestaoplanosvoo.application.usecases.AprovarPlanoUseCase;
import br.pucrs.projarc.gestaoplanosvoo.application.usecases.CancelarPlanoUseCase;
import br.pucrs.projarc.gestaoplanosvoo.application.usecases.ConsultarPlanoUseCase;
import br.pucrs.projarc.gestaoplanosvoo.application.usecases.CriarPlanoUseCase;
import br.pucrs.projarc.gestaoplanosvoo.application.usecases.EditarPercursoUseCase;
import br.pucrs.projarc.gestaoplanosvoo.application.usecases.ListarPlanosUseCase;
import br.pucrs.projarc.gestaoplanosvoo.application.usecases.ValidarPlanoUseCase;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.SituacaoPlano;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/planos")
@Tag(name = "Planos de voo")
public class PlanoDeVooController {

    private final CriarPlanoUseCase criarPlano;
    private final EditarPercursoUseCase editarPercurso;
    private final ValidarPlanoUseCase validarPlano;
    private final AprovarPlanoUseCase aprovarPlano;
    private final CancelarPlanoUseCase cancelarPlano;
    private final ConsultarPlanoUseCase consultarPlano;
    private final ListarPlanosUseCase listarPlanos;
    private final AbandonarPlanoUseCase abandonarPlano;

    public PlanoDeVooController(CriarPlanoUseCase criarPlano, EditarPercursoUseCase editarPercurso,
                                ValidarPlanoUseCase validarPlano, AprovarPlanoUseCase aprovarPlano,
                                CancelarPlanoUseCase cancelarPlano, ConsultarPlanoUseCase consultarPlano,
                                ListarPlanosUseCase listarPlanos, AbandonarPlanoUseCase abandonarPlano) {
        this.criarPlano = criarPlano;
        this.editarPercurso = editarPercurso;
        this.validarPlano = validarPlano;
        this.aprovarPlano = aprovarPlano;
        this.cancelarPlano = cancelarPlano;
        this.consultarPlano = consultarPlano;
        this.listarPlanos = listarPlanos;
        this.abandonarPlano = abandonarPlano;
    }

    @PostMapping
    @Operation(summary = "Criar plano de voo")
    @ApiResponse(responseCode = "201", description = "Plano criado")
    @ApiResponse(responseCode = "404", description = "Piloto, aeronave ou aeroporto inexistente")
    @ApiResponse(responseCode = "422", description = "Origem igual ao destino")
    public ResponseEntity<PlanoResponse> criar(@Valid @RequestBody CriarPlanoRequest req) {
        PlanoResponse plano = criarPlano.executar(req);
        URI local = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(plano.id()).toUri();
        return ResponseEntity.created(local).body(plano);
    }

    @GetMapping
    @Operation(summary = "Listar planos de voo", description = "Sem o parâmetro situacao, lista todos os planos.")
    @ApiResponse(responseCode = "200", description = "Planos encontrados (lista possivelmente vazia)")
    @ApiResponse(responseCode = "400", description = "Situação desconhecida")
    public List<PlanoResumoResponse> listar(
            @Parameter(description = "EM_EDICAO, APROVADO, CANCELADO ou ABANDONADO")
            @RequestParam(required = false) SituacaoPlano situacao) {
        return listarPlanos.executar(situacao);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar plano de voo")
    @ApiResponse(responseCode = "200", description = "Plano encontrado")
    @ApiResponse(responseCode = "404", description = "Plano inexistente")
    public PlanoResponse consultar(@PathVariable Long id) {
        return consultarPlano.executar(id);
    }

    @PostMapping("/{id}/trechos")
    @Operation(summary = "Adicionar trecho ao percurso")
    @ApiResponse(responseCode = "201", description = "Trecho acrescentado com sucesso")
    @ApiResponse(responseCode = "404", description = "Plano, aerovia ou vaga inexistente")
    @ApiResponse(responseCode = "409", description = "Plano fora de edição")
    @ApiResponse(responseCode = "422", description = "Altitude inválida")
    public ResponseEntity<PlanoResponse> adicionarTrecho(@PathVariable Long id, @Valid @RequestBody TrechoRequest req) {
        PlanoResponse plano = editarPercurso.adicionar(id, req);
        URI local = ServletUriComponentsBuilder.fromCurrentRequest().path("/{ordem}")
                .buildAndExpand(plano.trechos().size()).toUri();
        return ResponseEntity.created(local).body(plano);
    }

    @PutMapping("/{id}/trechos/{ordem}")
    @Operation(summary = "Alterar trecho do percurso")
    @ApiResponse(responseCode = "200", description = "Trecho substituído")
    @ApiResponse(responseCode = "404", description = "Plano, trecho, aerovia ou vaga inexistente")
    @ApiResponse(responseCode = "409", description = "Plano fora de edição")
    @ApiResponse(responseCode = "422", description = "Altitude inválida")
    public PlanoResponse alterarTrecho(@PathVariable Long id, @PathVariable int ordem, @Valid @RequestBody TrechoRequest req) {
        return editarPercurso.alterar(id, ordem, req);
    }

    @DeleteMapping("/{id}/trechos/{ordem}")
    @Operation(summary = "Remover trecho do percurso")
    @ApiResponse(responseCode = "200", description = "Trecho removido")
    @ApiResponse(responseCode = "404", description = "Plano ou trecho inexistente")
    @ApiResponse(responseCode = "409", description = "Plano fora de edição")
    public PlanoResponse removerTrecho(@PathVariable Long id, @PathVariable int ordem) {
        return editarPercurso.remover(id, ordem);
    }

    @PostMapping("/{id}/validacao")
    @Operation(summary = "Validar plano de voo")
    @ApiResponse(responseCode = "200", description = "Resultado da validação")
    @ApiResponse(responseCode = "404", description = "Plano inexistente")
    @ApiResponse(responseCode = "409", description = "Plano fora de edição")
    public ResultadoValidacaoResponse validar(@PathVariable Long id) {
        return validarPlano.executar(id);
    }

    @PostMapping("/{id}/aprovacao")
    @Operation(summary = "Aprovar plano de voo")
    @ApiResponse(responseCode = "200", description = "Plano aprovado com as reservas")
    @ApiResponse(responseCode = "404", description = "Plano inexistente")
    @ApiResponse(responseCode = "409", description = "Plano fora de edição ou conflito de reserva")
    @ApiResponse(responseCode = "422", description = "Plano com violações")
    public PlanoResponse aprovar(@PathVariable Long id) {
        return aprovarPlano.executar(id);
    }

    @PostMapping("/{id}/abandono")
    @Operation(summary = "Abandonar plano em edição")
    @ApiResponse(responseCode = "200", description = "Plano abandonado")
    @ApiResponse(responseCode = "404", description = "Plano inexistente")
    @ApiResponse(responseCode = "409", description = "Plano fora de edição")
    public PlanoResponse abandonar(@PathVariable Long id) {
        return abandonarPlano.executar(id);
    }

    @PostMapping("/{id}/cancelamento")
    @Operation(summary = "Cancelar plano de voo")
    @ApiResponse(responseCode = "200", description = "Plano cancelado")
    @ApiResponse(responseCode = "404", description = "Plano inexistente")
    @ApiResponse(responseCode = "409", description = "Plano não aprovado ou voo já executado")
    public PlanoResponse cancelar(@PathVariable Long id) {
        return cancelarPlano.executar(id);
    }
}
