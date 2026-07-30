package br.com.imoveis.infrastructure.rest;

import br.com.imoveis.application.usecase.ConfirmarPagamento;
import br.com.imoveis.infrastructure.auth.CurrentPrincipal;
import br.com.imoveis.infrastructure.auth.Principal;
import br.com.imoveis.infrastructure.rest.dto.ContratoDtos.PagamentoResponse;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;

import java.util.UUID;

@Controller("/pagamentos")
public class PagamentosController {

    private final ConfirmarPagamento confirmar;

    public PagamentosController(ConfirmarPagamento confirmar) {
        this.confirmar = confirmar;
    }

    @Post(value = "/{id}/confirmar", produces = MediaType.APPLICATION_JSON)
    public PagamentoResponse confirmar(@PathVariable UUID id, HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        return PagamentoResponse.from(confirmar.execute(id, p.proprietarioId()));
    }
}
