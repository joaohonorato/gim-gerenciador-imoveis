package br.com.imoveis.infrastructure.rest;

import br.com.imoveis.application.usecase.AprovarCandidato;
import br.com.imoveis.application.usecase.RecusarCandidato;
import br.com.imoveis.infrastructure.auth.CurrentPrincipal;
import br.com.imoveis.infrastructure.auth.Principal;
import br.com.imoveis.infrastructure.rest.dto.ContratoDtos.ContratoResponse;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Status;

import java.util.UUID;

@Controller("/candidaturas")
public class CandidaturasController {

    private final AprovarCandidato aprovar;
    private final RecusarCandidato recusar;

    public CandidaturasController(AprovarCandidato aprovar, RecusarCandidato recusar) {
        this.aprovar = aprovar;
        this.recusar = recusar;
    }

    @Status(HttpStatus.CREATED)
    @Post(value = "/{id}/aprovar", produces = MediaType.APPLICATION_JSON)
    public ContratoResponse aprovar(@PathVariable UUID id, HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        return ContratoResponse.from(aprovar.execute(id, p.proprietarioId()));
    }

    @Post(value = "/{id}/recusar", produces = MediaType.APPLICATION_JSON)
    public void recusar(@PathVariable UUID id, HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        recusar.execute(id, p.proprietarioId());
    }
}
