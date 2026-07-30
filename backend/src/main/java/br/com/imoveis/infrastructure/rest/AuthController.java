package br.com.imoveis.infrastructure.rest;

import br.com.imoveis.application.exception.AutenticacaoInvalidaException;
import br.com.imoveis.application.ports.ConviteAcessoRepository;
import br.com.imoveis.application.ports.InquilinoRepository;
import br.com.imoveis.application.ports.ProprietarioRepository;
import br.com.imoveis.application.usecase.AceitarConviteAcesso;
import br.com.imoveis.application.usecase.AutenticarConta;
import br.com.imoveis.application.usecase.CriarConviteAcessoProprietario;
import br.com.imoveis.application.usecase.EncerrarSessao;
import br.com.imoveis.application.usecase.RegistrarProprietarioAcesso;
import br.com.imoveis.domain.auth.ConviteAcesso;
import br.com.imoveis.domain.inquilino.Inquilino;
import br.com.imoveis.domain.proprietario.Proprietario;
import br.com.imoveis.infrastructure.auth.CurrentPrincipal;
import br.com.imoveis.infrastructure.auth.Principal;
import br.com.imoveis.infrastructure.rest.dto.AuthDtos.*;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import jakarta.validation.Valid;

import java.util.Optional;

@Controller("/auth")
public class AuthController {

    private final AutenticarConta autenticarConta;
    private final RegistrarProprietarioAcesso registrarProprietarioAcesso;
    private final CriarConviteAcessoProprietario criarConviteAcessoProprietario;
    private final AceitarConviteAcesso aceitarConviteAcesso;
    private final EncerrarSessao encerrarSessao;
    private final ConviteAcessoRepository conviteAcessoRepository;
    private final ProprietarioRepository proprietarioRepository;
    private final InquilinoRepository inquilinoRepository;

    public AuthController(AutenticarConta autenticarConta,
                          RegistrarProprietarioAcesso registrarProprietarioAcesso,
                          CriarConviteAcessoProprietario criarConviteAcessoProprietario,
                          AceitarConviteAcesso aceitarConviteAcesso,
                          EncerrarSessao encerrarSessao,
                          ConviteAcessoRepository conviteAcessoRepository,
                          ProprietarioRepository proprietarioRepository,
                          InquilinoRepository inquilinoRepository) {
        this.autenticarConta = autenticarConta;
        this.registrarProprietarioAcesso = registrarProprietarioAcesso;
        this.criarConviteAcessoProprietario = criarConviteAcessoProprietario;
        this.aceitarConviteAcesso = aceitarConviteAcesso;
        this.encerrarSessao = encerrarSessao;
        this.conviteAcessoRepository = conviteAcessoRepository;
        this.proprietarioRepository = proprietarioRepository;
        this.inquilinoRepository = inquilinoRepository;
    }

    @Post(value = "/register/proprietario", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    public LoginResponse registrarProprietario(@Body @Valid RegistrarProprietarioRequest req) {
        var result = registrarProprietarioAcesso.execute(req.email(), req.senha(), req.username(), req.cpfCnpj());
        return new LoginResponse(
            result.sessionToken(),
            result.proprietario().id(),
            result.proprietario().nome(),
            result.proprietario().email().value(),
            result.contaAcesso().tipo());
    }

    @Post(value = "/login", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    public LoginResponse login(@Body @Valid LoginRequest req) {
        AutenticarConta.Result result = autenticarConta.execute(req.email(), req.senha());
        Proprietario proprietario = result.proprietario();
        return new LoginResponse(
            result.sessionToken(),
            proprietario == null ? null : proprietario.id(),
            proprietario == null ? null : proprietario.nome(),
            result.contaAcesso().email().value(),
            result.contaAcesso().tipo());
    }

    @Post(value = "/convites/proprietarios", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    public ConviteAcessoResponse criarConviteProprietario(@Body @Valid NovoConviteProprietarioRequest req) {
        ConviteAcesso convite = criarConviteAcessoProprietario.execute(req.email(), req.nome(), req.cpfCnpj(), req.perfil());
        return toResponse(convite);
    }

    @Get(value = "/convites/{token}", produces = MediaType.APPLICATION_JSON)
    public ConviteAcessoResponse buscarConvite(@PathVariable String token) {
        ConviteAcesso convite = conviteAcessoRepository.findByToken(token)
            .orElseThrow(() -> new AutenticacaoInvalidaException("convite inválido"));
        return toResponse(convite);
    }

    @Post(value = "/convites/{token}/aceitar", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    public LoginResponse aceitarConvite(@PathVariable String token, @Body @Valid AceitarConviteRequest req) {
        AceitarConviteAcesso.Result result = aceitarConviteAcesso.execute(token, req.email(), req.senha());
        return new LoginResponse(
            result.sessionToken(),
            result.proprietario().id(),
            result.proprietario().nome(),
            result.proprietario().email().value(),
            br.com.imoveis.domain.auth.TipoContaAcesso.PROPRIETARIO);
    }

    @Get(value = "/me", produces = MediaType.APPLICATION_JSON)
    public MeResponse me(HttpRequest<?> request) {
        Principal principal = CurrentPrincipal.require(request);
        if (principal.inquilinoId() != null) {
            Inquilino inquilino = inquilinoRepository.findById(principal.inquilinoId())
                .orElseThrow(() -> new AutenticacaoInvalidaException("usuário autenticado não encontrado"));
            return new MeResponse(inquilino.id(), inquilino.nome(), inquilino.email().value(), principal.tipoConta());
        }

        Proprietario proprietario = principal.proprietarioId() == null ? null : proprietarioRepository.findById(principal.proprietarioId())
            .orElseThrow(() -> new AutenticacaoInvalidaException("usuário autenticado não encontrado"));
        if (proprietario == null) {
            return new MeResponse(null, null, null, principal.tipoConta());
        }
        return new MeResponse(proprietario.id(), proprietario.nome(), proprietario.email().value(), principal.tipoConta());
    }

    @Post(value = "/logout")
    @Status(HttpStatus.NO_CONTENT)
    public void logout(HttpRequest<?> request) {
        CurrentPrincipal.require(request);
        String token = extractToken(request)
            .orElseThrow(() -> new AutenticacaoInvalidaException("autenticação obrigatória"));
        encerrarSessao.execute(token);
    }

    @Get(value = "/ping", produces = MediaType.APPLICATION_JSON)
    public java.util.Map<String, String> ping() {
        return java.util.Map.of("status", "ok");
    }

    private static ConviteAcessoResponse toResponse(ConviteAcesso convite) {
        return new ConviteAcessoResponse(
            convite.token(),
            convite.email().value(),
            convite.tipo(),
            convite.nome(),
            convite.documento(),
            convite.perfilProprietario(),
            convite.consumido());
    }

    private static Optional<String> extractToken(HttpRequest<?> request) {
        return request.getHeaders().getAuthorization()
            .filter(h -> h.startsWith("Bearer "))
            .map(h -> h.substring("Bearer ".length()).trim())
            .filter(t -> !t.isBlank());
    }
}
