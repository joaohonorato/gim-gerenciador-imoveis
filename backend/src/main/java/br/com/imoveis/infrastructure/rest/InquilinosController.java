package br.com.imoveis.infrastructure.rest;

import br.com.imoveis.application.usecase.BuscarInquilinoDoProprietario;
import br.com.imoveis.domain.inquilino.Inquilino;
import br.com.imoveis.infrastructure.auth.CurrentPrincipal;
import br.com.imoveis.infrastructure.auth.Principal;
import br.com.imoveis.infrastructure.rest.dto.InquilinoDtos.InquilinoResponse;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.UUID;

@Controller("/inquilinos")
@Tag(name = "Inquilinos")
public class InquilinosController {

    private final BuscarInquilinoDoProprietario buscar;

    public InquilinosController(BuscarInquilinoDoProprietario buscar) {
        this.buscar = buscar;
    }

    @Get(value = "/{id}", produces = MediaType.APPLICATION_JSON)
    public InquilinoResponse detalhe(@PathVariable UUID id, HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        Inquilino inquilino = buscar.execute(id, p.requireProprietarioId());
        return InquilinoResponse.from(inquilino);
    }
}
