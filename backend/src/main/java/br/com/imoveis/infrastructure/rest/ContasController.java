package br.com.imoveis.infrastructure.rest;

import br.com.imoveis.application.exception.NaoEncontradoException;
import br.com.imoveis.application.ports.ImovelRepository;
import br.com.imoveis.application.usecase.RegistrarConta;
import br.com.imoveis.domain.imovel.Conta;
import br.com.imoveis.domain.imovel.Imovel;
import br.com.imoveis.domain.shared.Dinheiro;
import br.com.imoveis.infrastructure.auth.CurrentPrincipal;
import br.com.imoveis.infrastructure.auth.Principal;
import br.com.imoveis.infrastructure.rest.dto.ContaDtos.*;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

@Controller
@Tag(name = "Contas")
public class ContasController {

    private final RegistrarConta registrarConta;
    private final ImovelRepository imovelRepository;

    public ContasController(RegistrarConta registrarConta, ImovelRepository imovelRepository) {
        this.registrarConta = registrarConta;
        this.imovelRepository = imovelRepository;
    }

    @Get(value = "/imoveis/{imovelId}/contas", produces = MediaType.APPLICATION_JSON)
    public List<ContaResponse> listar(@PathVariable UUID imovelId, HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        Imovel imovel = imovelRepository.findById(imovelId)
            .filter(i -> i.proprietarioId().equals(p.proprietarioId()))
            .orElseThrow(() -> new NaoEncontradoException("imóvel"));
        return imovelRepository.findContasByImovel(imovel.id()).stream().map(ContaResponse::from).toList();
    }

    @Post(value = "/imoveis/{imovelId}/contas", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    public ContaResponse criar(@PathVariable UUID imovelId, @Body NovaContaRequest body, HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        Conta conta = registrarConta.execute(imovelId, p.proprietarioId(), body.tipo(), body.vencimento(),
            new Dinheiro(body.valor()));
        return ContaResponse.from(conta);
    }

    @Patch(value = "/contas/{id}", produces = MediaType.APPLICATION_JSON)
    public ContaResponse marcarPaga(@PathVariable UUID id, HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        Conta conta = imovelRepository.findContaById(id)
            .orElseThrow(() -> new NaoEncontradoException("conta"));
        Imovel imovel = imovelRepository.findById(conta.imovelId())
            .filter(i -> i.proprietarioId().equals(p.proprietarioId()))
            .orElseThrow(() -> new NaoEncontradoException("conta"));
        conta.marcarPaga();
        return ContaResponse.from(imovelRepository.saveConta(conta));
    }
}
