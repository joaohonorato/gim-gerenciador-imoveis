package br.com.imoveis.infrastructure.rest;

import br.com.imoveis.application.exception.NaoEncontradoException;
import br.com.imoveis.application.ports.ContratoRepository;
import br.com.imoveis.application.usecase.AssinarContrato;
import br.com.imoveis.domain.contrato.Contrato;
import br.com.imoveis.infrastructure.auth.CurrentPrincipal;
import br.com.imoveis.infrastructure.auth.Principal;
import br.com.imoveis.infrastructure.rest.dto.ContratoDtos.*;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;

import java.util.List;
import java.util.UUID;

@Controller("/contratos")
public class ContratosController {

    private final AssinarContrato assinar;
    private final ContratoRepository contratoRepository;

    public ContratosController(AssinarContrato assinar, ContratoRepository contratoRepository) {
        this.assinar = assinar;
        this.contratoRepository = contratoRepository;
    }

    @Get(produces = MediaType.APPLICATION_JSON)
    public List<ContratoResponse> listar(HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        List<Contrato> contratos = p.inquilinoId() != null
            ? contratoRepository.findByInquilinoId(p.inquilinoId())
            : contratoRepository.findByProprietarioId(p.requireProprietarioId());
        return contratos.stream().map(ContratoResponse::from).toList();
    }

    @Get(value = "/{id}", produces = MediaType.APPLICATION_JSON)
    public ContratoResponse detalhe(@PathVariable UUID id, HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        Contrato c = contratoRepository.findById(id)
            .filter(x -> isOwnerOrTenant(p, x))
            .orElseThrow(() -> new NaoEncontradoException("contrato"));
        return ContratoResponse.from(c);
    }

    @Post(value = "/{id}/assinar", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    public ContratoResponse assinar(@PathVariable UUID id, @Body AssinarRequest body, HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        Contrato contrato = contratoRepository.findById(id)
            .filter(x -> isOwnerOrTenant(p, x))
            .orElseThrow(() -> new NaoEncontradoException("contrato"));

        if (body.parte() == br.com.imoveis.domain.contrato.ParteContrato.PROPRIETARIO) {
            if (p.proprietarioId() == null || !contrato.proprietarioId().equals(p.proprietarioId())) {
                throw new NaoEncontradoException("contrato");
            }
        }

        if (body.parte() == br.com.imoveis.domain.contrato.ParteContrato.INQUILINO) {
            if (p.inquilinoId() == null || !contrato.inquilinoId().equals(p.inquilinoId())) {
                throw new NaoEncontradoException("contrato");
            }
        }

        return ContratoResponse.from(assinar.execute(id, body.parte()));
    }

    @Get(value = "/{id}/pagamentos", produces = MediaType.APPLICATION_JSON)
    public List<PagamentoResponse> pagamentos(@PathVariable UUID id, HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        contratoRepository.findById(id)
            .filter(x -> isOwnerOrTenant(p, x))
            .orElseThrow(() -> new NaoEncontradoException("contrato"));
        return contratoRepository.findPagamentosByContrato(id).stream().map(PagamentoResponse::from).toList();
    }

    private boolean isOwnerOrTenant(Principal p, Contrato c) {
        return (p.proprietarioId() != null && c.proprietarioId().equals(p.proprietarioId()))
            || (p.inquilinoId() != null && c.inquilinoId().equals(p.inquilinoId()));
    }
}
