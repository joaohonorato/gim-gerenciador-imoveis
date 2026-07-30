package br.com.imoveis.infrastructure.rest;

import br.com.imoveis.application.exception.AutenticacaoInvalidaException;
import br.com.imoveis.application.exception.ConflitoException;
import br.com.imoveis.application.exception.NaoEncontradoException;
import br.com.imoveis.domain.imovel.TransicaoInvalidaException;
import br.com.imoveis.infrastructure.rest.dto.ErrorResponse;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Error;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.annotation.Controller;
import jakarta.validation.ConstraintViolationException;

import java.util.stream.Collectors;

@Controller
@Produces(MediaType.APPLICATION_JSON)
public class GlobalErrorHandler {

    @Error(global = true, exception = NaoEncontradoException.class)
    public HttpResponse<ErrorResponse> notFound(HttpRequest<?> req, NaoEncontradoException ex) {
        return HttpResponse.notFound(new ErrorResponse("NOT_FOUND", ex.getMessage()));
    }

    @Error(global = true, exception = ConflitoException.class)
    public HttpResponse<ErrorResponse> conflito(HttpRequest<?> req, ConflitoException ex) {
        return HttpResponse.<ErrorResponse>status(io.micronaut.http.HttpStatus.CONFLICT)
            .body(new ErrorResponse("CONFLICT", ex.getMessage()));
    }

    @Error(global = true, exception = AutenticacaoInvalidaException.class)
    public HttpResponse<ErrorResponse> auth(HttpRequest<?> req, AutenticacaoInvalidaException ex) {
        return HttpResponse.<ErrorResponse>status(io.micronaut.http.HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse("AUTH_INVALID", ex.getMessage()));
    }

    @Error(global = true, exception = TransicaoInvalidaException.class)
    public HttpResponse<ErrorResponse> transicao(HttpRequest<?> req, TransicaoInvalidaException ex) {
        return HttpResponse.<ErrorResponse>status(io.micronaut.http.HttpStatus.CONFLICT)
            .body(new ErrorResponse("INVALID_TRANSITION", ex.getMessage()));
    }

    @Error(global = true, exception = IllegalArgumentException.class)
    public HttpResponse<ErrorResponse> illegalArg(HttpRequest<?> req, IllegalArgumentException ex) {
        return HttpResponse.badRequest(new ErrorResponse("INVALID_INPUT", ex.getMessage()));
    }

    @Error(global = true, exception = IllegalStateException.class)
    public HttpResponse<ErrorResponse> illegalState(HttpRequest<?> req, IllegalStateException ex) {
        return HttpResponse.<ErrorResponse>status(io.micronaut.http.HttpStatus.CONFLICT)
            .body(new ErrorResponse("INVALID_STATE", ex.getMessage()));
    }

    @Error(global = true, exception = ConstraintViolationException.class)
    public HttpResponse<ErrorResponse> validation(HttpRequest<?> req, ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
            .map(v -> v.getPropertyPath() + ": " + v.getMessage())
            .sorted()
            .collect(Collectors.joining("; "));
        if (message.isBlank()) {
            message = "dados inválidos";
        }
        return HttpResponse.badRequest(new ErrorResponse("VALIDATION_ERROR", message));
    }
}
