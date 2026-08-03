package br.com.imoveis.infrastructure.rest;

import br.com.imoveis.application.exception.AutenticacaoInvalidaException;
import br.com.imoveis.application.ports.ArquivoRepository;
import br.com.imoveis.application.ports.ArquivoStorage;
import br.com.imoveis.application.ports.ContaAcessoRepository;
import br.com.imoveis.application.ports.ConviteAcessoRepository;
import br.com.imoveis.application.ports.InquilinoRepository;
import br.com.imoveis.application.ports.ProprietarioRepository;
import br.com.imoveis.application.usecase.AceitarConviteAcesso;
import br.com.imoveis.application.usecase.AtualizarAvatar;
import br.com.imoveis.application.usecase.AutenticarConta;
import br.com.imoveis.application.usecase.ConfirmarVerificacaoEmail;
import br.com.imoveis.application.usecase.CriarConviteAcessoProprietario;
import br.com.imoveis.application.usecase.EncerrarSessao;
import br.com.imoveis.application.usecase.RedefinirSenha;
import br.com.imoveis.application.usecase.ReenviarVerificacaoEmail;
import br.com.imoveis.application.usecase.RegistrarProprietarioAcesso;
import br.com.imoveis.application.usecase.SolicitarRedefinicaoSenha;
import io.swagger.v3.oas.annotations.tags.Tag;
import br.com.imoveis.domain.arquivo.Arquivo;
import br.com.imoveis.domain.arquivo.TipoArquivo;
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
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import jakarta.validation.Valid;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller("/auth")
@Tag(name = "Autenticação")
public class AuthController {

    private final AutenticarConta autenticarConta;
    private final RegistrarProprietarioAcesso registrarProprietarioAcesso;
    private final CriarConviteAcessoProprietario criarConviteAcessoProprietario;
    private final AceitarConviteAcesso aceitarConviteAcesso;
    private final EncerrarSessao encerrarSessao;
    private final AtualizarAvatar atualizarAvatar;
    private final SolicitarRedefinicaoSenha solicitarRedefinicaoSenha;
    private final RedefinirSenha redefinirSenha;
    private final ConfirmarVerificacaoEmail confirmarVerificacaoEmail;
    private final ReenviarVerificacaoEmail reenviarVerificacaoEmail;
    private final ConviteAcessoRepository conviteAcessoRepository;
    private final ProprietarioRepository proprietarioRepository;
    private final InquilinoRepository inquilinoRepository;
    private final ContaAcessoRepository contaAcessoRepository;
    private final ArquivoRepository arquivoRepository;
    private final ArquivoStorage arquivoStorage;

    public AuthController(AutenticarConta autenticarConta,
                          RegistrarProprietarioAcesso registrarProprietarioAcesso,
                          CriarConviteAcessoProprietario criarConviteAcessoProprietario,
                          AceitarConviteAcesso aceitarConviteAcesso,
                          EncerrarSessao encerrarSessao,
                          AtualizarAvatar atualizarAvatar,
                          SolicitarRedefinicaoSenha solicitarRedefinicaoSenha,
                          RedefinirSenha redefinirSenha,
                          ConfirmarVerificacaoEmail confirmarVerificacaoEmail,
                          ReenviarVerificacaoEmail reenviarVerificacaoEmail,
                          ConviteAcessoRepository conviteAcessoRepository,
                          ProprietarioRepository proprietarioRepository,
                          InquilinoRepository inquilinoRepository,
                          ContaAcessoRepository contaAcessoRepository,
                          ArquivoRepository arquivoRepository,
                          ArquivoStorage arquivoStorage) {
        this.autenticarConta = autenticarConta;
        this.registrarProprietarioAcesso = registrarProprietarioAcesso;
        this.criarConviteAcessoProprietario = criarConviteAcessoProprietario;
        this.aceitarConviteAcesso = aceitarConviteAcesso;
        this.encerrarSessao = encerrarSessao;
        this.atualizarAvatar = atualizarAvatar;
        this.solicitarRedefinicaoSenha = solicitarRedefinicaoSenha;
        this.redefinirSenha = redefinirSenha;
        this.confirmarVerificacaoEmail = confirmarVerificacaoEmail;
        this.reenviarVerificacaoEmail = reenviarVerificacaoEmail;
        this.conviteAcessoRepository = conviteAcessoRepository;
        this.proprietarioRepository = proprietarioRepository;
        this.inquilinoRepository = inquilinoRepository;
        this.contaAcessoRepository = contaAcessoRepository;
        this.arquivoRepository = arquivoRepository;
        this.arquivoStorage = arquivoStorage;
    }

    @Post(value = "/register/proprietario", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    public LoginResponse registrarProprietario(@Body @Valid RegistrarProprietarioRequest req) {
        var result = registrarProprietarioAcesso.execute(req.email(), req.senha(), req.username(), req.cpfCnpj());
        return new LoginResponse(
            result.sessionToken(),
            result.proprietario().id(),
            result.proprietario().nome(),
            result.proprietario().email().value(),
            result.contaAcesso().tipo(),
            result.contaAcesso().emailVerificado());
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
            result.contaAcesso().tipo(),
            result.contaAcesso().emailVerificado());
    }

    @Post(value = "/senha/esqueci", consumes = MediaType.APPLICATION_JSON)
    @Status(HttpStatus.NO_CONTENT)
    public void esqueciSenha(@Body @Valid EsqueciSenhaRequest req) {
        solicitarRedefinicaoSenha.execute(req.email());
    }

    @Post(value = "/senha/redefinir", consumes = MediaType.APPLICATION_JSON)
    @Status(HttpStatus.NO_CONTENT)
    public void redefinirSenha(@Body @Valid RedefinirSenhaRequest req) {
        redefinirSenha.execute(req.token(), req.novaSenha());
    }

    @Post(value = "/email/confirmar", consumes = MediaType.APPLICATION_JSON)
    @Status(HttpStatus.NO_CONTENT)
    public void confirmarEmail(@Body @Valid ConfirmarEmailRequest req) {
        confirmarVerificacaoEmail.execute(req.token());
    }

    @Post(value = "/email/reenviar")
    @Status(HttpStatus.NO_CONTENT)
    public void reenviarEmail(HttpRequest<?> request) {
        Principal principal = CurrentPrincipal.require(request);
        reenviarVerificacaoEmail.execute(principal.contaAcessoId());
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
            br.com.imoveis.domain.auth.TipoContaAcesso.PROPRIETARIO,
            false);
    }

    @Get(value = "/me", produces = MediaType.APPLICATION_JSON)
    public MeResponse me(HttpRequest<?> request) {
        return buildMeResponse(CurrentPrincipal.require(request));
    }

    @ExecuteOn(TaskExecutors.BLOCKING)
    @Post(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA, produces = MediaType.APPLICATION_JSON)
    public MeResponse atualizarAvatar(CompletedFileUpload avatar, HttpRequest<?> request) {
        Principal principal = CurrentPrincipal.require(request);
        TipoArquivo tipo = principal.inquilinoId() != null ? TipoArquivo.AVATAR_INQUILINO : TipoArquivo.AVATAR_PROPRIETARIO;
        UUID donoId = principal.inquilinoId() != null ? principal.inquilinoId() : principal.requireProprietarioId();
        String contentType = avatar.getContentType().map(MediaType::toString).orElse(null);
        try {
            atualizarAvatar.execute(tipo, donoId, avatar.getFilename(), contentType, avatar.getSize(), avatar.getInputStream());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return buildMeResponse(principal);
    }

    private MeResponse buildMeResponse(Principal principal) {
        boolean emailVerificado = contaAcessoRepository.findById(principal.contaAcessoId())
            .map(br.com.imoveis.domain.auth.ContaAcesso::emailVerificado)
            .orElse(false);

        if (principal.inquilinoId() != null) {
            Inquilino inquilino = inquilinoRepository.findById(principal.inquilinoId())
                .orElseThrow(() -> new AutenticacaoInvalidaException("usuário autenticado não encontrado"));
            String avatarUrl = avatarUrl(TipoArquivo.AVATAR_INQUILINO, inquilino.id());
            return new MeResponse(inquilino.id(), inquilino.nome(), inquilino.email().value(), principal.tipoConta(), avatarUrl, emailVerificado);
        }

        Proprietario proprietario = principal.proprietarioId() == null ? null : proprietarioRepository.findById(principal.proprietarioId())
            .orElseThrow(() -> new AutenticacaoInvalidaException("usuário autenticado não encontrado"));
        if (proprietario == null) {
            return new MeResponse(null, null, null, principal.tipoConta(), null, emailVerificado);
        }
        String avatarUrl = avatarUrl(TipoArquivo.AVATAR_PROPRIETARIO, proprietario.id());
        return new MeResponse(proprietario.id(), proprietario.nome(), proprietario.email().value(), principal.tipoConta(), avatarUrl, emailVerificado);
    }

    private String avatarUrl(TipoArquivo tipo, UUID donoId) {
        List<Arquivo> arquivos = arquivoRepository.findByDonoIdAndTipo(donoId, tipo);
        if (arquivos.isEmpty()) return null;
        Arquivo avatar = arquivos.get(0);
        return arquivoStorage.urlPublica(avatar.tipo().container(), avatar.blobKey());
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
