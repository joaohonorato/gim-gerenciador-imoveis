package br.com.imoveis.infrastructure.rest;

import br.com.imoveis.application.exception.NaoEncontradoException;
import br.com.imoveis.application.ports.ImovelRepository;
import br.com.imoveis.application.ports.TipoContaRepository;
import br.com.imoveis.application.usecase.RegistrarConta;
import br.com.imoveis.application.usecase.RegistrarTipoConta;
import br.com.imoveis.domain.imovel.Conta;
import br.com.imoveis.domain.imovel.Imovel;
import br.com.imoveis.domain.imovel.TipoConta;
import br.com.imoveis.domain.shared.Dinheiro;
import br.com.imoveis.infrastructure.auth.CurrentPrincipal;
import br.com.imoveis.infrastructure.auth.Principal;
import br.com.imoveis.infrastructure.rest.dto.ContaDtos.*;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

@Controller
@Tag(name = "Contas")
public class ContasController {

    private final RegistrarConta registrarConta;
    private final RegistrarTipoConta registrarTipoConta;
    private final ImovelRepository imovelRepository;
    private final TipoContaRepository tipoContaRepository;

    public ContasController(RegistrarConta registrarConta, RegistrarTipoConta registrarTipoConta,
                             ImovelRepository imovelRepository, TipoContaRepository tipoContaRepository) {
        this.registrarConta = registrarConta;
        this.registrarTipoConta = registrarTipoConta;
        this.imovelRepository = imovelRepository;
        this.tipoContaRepository = tipoContaRepository;
    }

    @Get(value = "/imoveis/{imovelId}/contas", produces = MediaType.APPLICATION_JSON)
    public List<ContaResponse> listar(@PathVariable UUID imovelId, HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        Imovel imovel = imovelRepository.findById(imovelId)
            .filter(i -> i.proprietarioId().equals(p.proprietarioId()))
            .orElseThrow(() -> new NaoEncontradoException("imóvel"));
        return imovelRepository.findContasByImovel(imovel.id()).stream().map(this::toResponse).toList();
    }

    @Post(value = "/imoveis/{imovelId}/contas", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    public ContaResponse criar(@PathVariable UUID imovelId, @Body NovaContaRequest body, HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        Conta conta = registrarConta.execute(imovelId, p.proprietarioId(), body.tipoContaId(), body.vencimento(),
            new Dinheiro(body.valor()), body.responsavel(), body.contratoId());
        return toResponse(conta);
    }

    @Patch(value = "/contas/{id}", produces = MediaType.APPLICATION_JSON)
    public ContaResponse marcarPaga(@PathVariable UUID id, HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        Conta conta = imovelRepository.findContaById(id)
            .orElseThrow(() -> new NaoEncontradoException("conta"));
        imovelRepository.findUnidadeById(conta.unidadeId())
            .flatMap(unidade -> imovelRepository.findById(unidade.imovelId()))
            .filter(i -> i.proprietarioId().equals(p.proprietarioId()))
            .orElseThrow(() -> new NaoEncontradoException("conta"));
        conta.marcarPaga();
        return toResponse(imovelRepository.saveConta(conta));
    }

    @Get(value = "/tipos-conta", produces = MediaType.APPLICATION_JSON)
    public List<TipoContaResponse> listarTipos(HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        return tipoContaRepository.findByProprietarioId(p.requireProprietarioId()).stream()
            .map(tc -> new TipoContaResponse(tc.id(), tc.nome()))
            .toList();
    }

    @Status(HttpStatus.CREATED)
    @Post(value = "/tipos-conta", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    public TipoContaResponse criarTipo(@Body @Valid NovoTipoContaRequest body, HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        TipoConta tipoConta = registrarTipoConta.execute(p.requireProprietarioId(), body.nome());
        return new TipoContaResponse(tipoConta.id(), tipoConta.nome());
    }

    private ContaResponse toResponse(Conta conta) {
        String tipoContaNome = tipoContaRepository.findById(conta.tipoContaId())
            .map(TipoConta::nome)
            .orElse(null);
        return ContaResponse.from(conta, tipoContaNome);
    }
}
