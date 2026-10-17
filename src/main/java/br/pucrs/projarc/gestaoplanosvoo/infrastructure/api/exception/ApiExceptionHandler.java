package br.pucrs.projarc.gestaoplanosvoo.infrastructure.api.exception;

import br.pucrs.projarc.gestaoplanosvoo.application.dto.response.ViolacaoResponse;
import br.pucrs.projarc.gestaoplanosvoo.domain.exception.ConflitoReservaException;
import br.pucrs.projarc.gestaoplanosvoo.domain.exception.DominioException;
import br.pucrs.projarc.gestaoplanosvoo.domain.exception.PlanoInvalidoException;
import br.pucrs.projarc.gestaoplanosvoo.domain.exception.RecursoNaoEncontradoException;
import br.pucrs.projarc.gestaoplanosvoo.domain.exception.RegraDeNegocioException;
import br.pucrs.projarc.gestaoplanosvoo.domain.exception.SituacaoInvalidaException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ProblemDetail naoEncontrado(RecursoNaoEncontradoException e) {
        return problema(HttpStatus.NOT_FOUND, "Recurso não encontrado", e);
    }

    @ExceptionHandler(SituacaoInvalidaException.class)
    ProblemDetail situacaoInvalida(SituacaoInvalidaException e) {
        return problema(HttpStatus.CONFLICT, "Operação incompatível com a situação do plano", e);
    }

    @ExceptionHandler(ConflitoReservaException.class)
    ProblemDetail conflitoReserva(ConflitoReservaException e) {
        return problema(HttpStatus.CONFLICT, "Conflito de reserva", e);
    }

    @ExceptionHandler(PlanoInvalidoException.class)
    ProblemDetail planoInvalido(PlanoInvalidoException e) {
        ProblemDetail p = problema(HttpStatus.UNPROCESSABLE_ENTITY, "Plano inválido", e);
        p.setProperty("violacoes", e.getResultado().violacoes().stream().map(ViolacaoResponse::de).toList());
        return p;
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    ProblemDetail regraDeNegocio(RegraDeNegocioException e) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "Regra de negócio violada", e);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail edicaoConcorrente(ObjectOptimisticLockingFailureException e) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "O plano foi alterado por outra requisição. Consulte-o novamente e repita a operação.");
        p.setTitle("Edição concorrente");
        p.setProperty("codigo", "EDICAO_CONCORRENTE");
        return p;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail integridade(DataIntegrityViolationException e) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "A operação viola uma restrição de integridade do banco e foi desfeita.");
        p.setTitle("Conflito de dados");
        p.setProperty("codigo", "CONFLITO_DADOS");
        return p;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Requisição com campos inválidos.");
        p.setTitle("Requisição inválida");
        p.setProperty("codigo", "REQUISICAO_INVALIDA");
        p.setProperty("campos", e.getBindingResult().getFieldErrors().stream()
                .map(f -> Map.of("campo", f.getField(), "erro", String.valueOf(f.getDefaultMessage())))
                .toList());
        return ResponseEntity.badRequest().body(p);
    }

    private ProblemDetail problema(HttpStatus status, String titulo, DominioException e) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(status, e.getMessage());
        p.setTitle(titulo);
        p.setType(URI.create("urn:planosvoo:erro:" + e.getCodigo().toLowerCase().replace('_', '-')));
        p.setProperty("codigo", e.getCodigo());
        return p;
    }
}
