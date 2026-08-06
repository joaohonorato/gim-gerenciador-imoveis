package br.com.imoveis.infrastructure.rest;

import br.com.imoveis.application.exception.AutenticacaoInvalidaException;
import br.com.imoveis.application.ports.ArquivoRepository;
import br.com.imoveis.application.ports.ArquivoStorage;
import br.com.imoveis.application.ports.Clock;
import br.com.imoveis.application.ports.ContaAcessoRepository;
import br.com.imoveis.application.ports.ConviteAcessoRepository;
import br.com.imoveis.application.ports.InquilinoRepository;
import br.com.imoveis.application.ports.ProprietarioRepository;
import br.com.imoveis.application.usecase.AceitarConviteAcesso;
import br.com.imoveis.application.usecase.AtualizarAvatar;
import br.com.imoveis.application.usecase.AtualizarTelefoneProprietario;
import br.com.imoveis.application.usecase.AutenticarConta;
import br.com.imoveis.application.usecase.CompletarCadastroProprietario;
import br.com.imoveis.application.usecase.ConfirmarVerificacaoEmail;
import br.com.imoveis.application.usecase.CriarConviteAcessoProprietario;
import br.com.imoveis.application.usecase.EncerrarSessao;
import br.com.imoveis.application.usecase.RedefinirSenha;
import br.com.imoveis.application.usecase.ReenviarVerificacaoEmail;
import br.com.imoveis.application.usecase.RegistrarProprietarioAcesso;
import br.com.imoveis.application.usecase.RegistrarPushToken;
import br.com.imoveis.application.usecase.RenomearInquilino;
import br.com.imoveis.application.usecase.RenomearProprietario;
import br.com.imoveis.application.usecase.SolicitarAlteracaoEmail;
import br.com.imoveis.application.usecase.SolicitarRedefinicaoSenha;
import br.com.imoveis.application.usecase.TrocarSenha;
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
    private final CompletarCadastroProprietario completarCadastroProprietario;
    private final AtualizarTelefoneProprietario atualizarTelefoneProprietario;
    private final TrocarSenha trocarSenha;
    private final RenomearProprietario renomearProprietario;
    private final RenomearInquilino renomearInquilino;
    private final SolicitarAlteracaoEmail solicitarAlteracaoEmail;
    private final RegistrarPushToken registrarPushToken;
    private final ConviteAcessoRepository conviteAcessoRepository;
    private final ProprietarioRepository proprietarioRepository;
    private final InquilinoRepository inquilinoRepository;
    private final ContaAcessoRepository contaAcessoRepository;
    private final ArquivoRepository arquivoRepository;
    private final ArquivoStorage arquivoStorage;
    private final Clock clock;

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
                          CompletarCadastroProprietario completarCadastroProprietario,
                          AtualizarTelefoneProprietario atualizarTelefoneProprietario,
                          TrocarSenha trocarSenha,
                          RenomearProprietario renomearProprietario,
                          RenomearInquilino renomearInquilino,
                          SolicitarAlteracaoEmail solicitarAlteracaoEmail,
                          RegistrarPushToken registrarPushToken,
                          ConviteAcessoRepository conviteAcessoRepository,
                          ProprietarioRepository proprietarioRepository,
                          InquilinoRepository inquilinoRepository,
                          ContaAcessoRepository contaAcessoRepository,
                          ArquivoRepository arquivoRepository,
                          ArquivoStorage arquivoStorage,
                          Clock clock) {
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
        this.completarCadastroProprietario = completarCadastroProprietario;
        this.atualizarTelefoneProprietario = atualizarTelefoneProprietario;
        this.trocarSenha = trocarSenha;
        this.renomearProprietario = renomearProprietario;
        this.renomearInquilino = renomearInquilino;
        this.solicitarAlteracaoEmail = solicitarAlteracaoEmail;
        this.registrarPushToken = registrarPushToken;
        this.conviteAcessoRepository = conviteAcessoRepository;
        this.proprietarioRepository = proprietarioRepository;
        this.inquilinoRepository = inquilinoRepository;
        this.contaAcessoRepository = contaAcessoRepository;
        this.arquivoRepository = arquivoRepository;
        this.arquivoStorage = arquivoStorage;
        this.clock = clock;
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

    // Diferente de /senha/redefinir (fluxo de "esqueci minha senha", via
    // token de e-mail, deslogado): aqui o usuário já está autenticado e
    // troca a senha proativamente, provando que conhece a senha atual.
    @Post(value = "/senha/trocar", consumes = MediaType.APPLICATION_JSON)
    @Status(HttpStatus.NO_CONTENT)
    public void trocarSenha(@Body @Valid TrocarSenhaRequest req, HttpRequest<?> request) {
        Principal principal = CurrentPrincipal.require(request);
        trocarSenha.execute(principal.contaAcessoId(), req.senhaAtual(), req.novaSenha());
    }

    // Registro de push token do device autenticado — usado pelo job diário
    // de alertas de vencimento (ver infrastructure/scheduling/
    // AlertasVencimentoScheduler) pra decidir quem tem device pra notificar
    // via push, além do e-mail. Idempotente: reenviar o mesmo token só
    // atualiza o timestamp/dono (ver PushTokenRepositoryAdapter).
    @Post(value = "/push-token", consumes = MediaType.APPLICATION_JSON)
    @Status(HttpStatus.NO_CONTENT)
    public void registrarPushToken(@Body @Valid RegistrarPushTokenRequest req, HttpRequest<?> request) {
        Principal principal = CurrentPrincipal.require(request);
        registrarPushToken.execute(principal.contaAcessoId(), req.token());
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

    @Put(value = "/me/cpf-cnpj", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    public MeResponse atualizarCpfCnpj(@Body @Valid AtualizarCpfCnpjRequest req, HttpRequest<?> request) {
        Principal principal = CurrentPrincipal.require(request);
        UUID proprietarioId = principal.proprietarioId();
        if (proprietarioId == null) {
            throw new AutenticacaoInvalidaException("cadastro de cpf/cnpj disponível só para proprietários");
        }
        completarCadastroProprietario.execute(proprietarioId, req.cpfCnpj());
        return buildMeResponse(principal);
    }

    @Put(value = "/me/telefone", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    public MeResponse atualizarTelefone(@Body @Valid AtualizarTelefoneRequest req, HttpRequest<?> request) {
        Principal principal = CurrentPrincipal.require(request);
        UUID proprietarioId = principal.proprietarioId();
        if (proprietarioId == null) {
            throw new AutenticacaoInvalidaException("cadastro de telefone disponível só para proprietários");
        }
        atualizarTelefoneProprietario.execute(proprietarioId, req.telefone());
        return buildMeResponse(principal);
    }

    @Put(value = "/me/nome", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    public MeResponse atualizarNome(@Body @Valid AtualizarNomeRequest req, HttpRequest<?> request) {
        Principal principal = CurrentPrincipal.require(request);
        if (principal.proprietarioId() != null) {
            renomearProprietario.execute(principal.proprietarioId(), req.nome());
        } else {
            renomearInquilino.execute(principal.requireInquilinoId(), req.nome());
        }
        return buildMeResponse(principal);
    }

    // Não aplica na hora — dispara um e-mail de confirmação pro *novo*
    // endereço e só troca o e-mail de login quando esse link for aberto (ver
    // SolicitarAlteracaoEmail/ConfirmarVerificacaoEmail). Por isso devolve
    // 204 em vez do MeResponse atualizado: nada muda em `me` até a
    // confirmação, só `emailPendente` passa a apontar pro novo endereço.
    @Post(value = "/me/email", consumes = MediaType.APPLICATION_JSON)
    @Status(HttpStatus.NO_CONTENT)
    public void solicitarAlteracaoEmail(@Body @Valid SolicitarAlteracaoEmailRequest req, HttpRequest<?> request) {
        Principal principal = CurrentPrincipal.require(request);
        solicitarAlteracaoEmail.execute(principal.contaAcessoId(), req.novoEmail());
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
        br.com.imoveis.domain.auth.ContaAcesso contaAcesso = contaAcessoRepository.findById(principal.contaAcessoId())
            .orElse(null);
        boolean emailVerificado = contaAcesso != null && contaAcesso.emailVerificado();
        String emailPendente = contaAcesso != null && contaAcesso.emailPendente() != null
            ? contaAcesso.emailPendente().value() : null;

        if (principal.inquilinoId() != null) {
            Inquilino inquilino = inquilinoRepository.findById(principal.inquilinoId())
                .orElseThrow(() -> new AutenticacaoInvalidaException("usuário autenticado não encontrado"));
            String avatarUrl = avatarUrl(TipoArquivo.AVATAR_INQUILINO, inquilino.id());
            return new MeResponse(inquilino.id(), inquilino.nome(), inquilino.email().value(), principal.tipoConta(),
                avatarUrl, emailVerificado, inquilino.cpf().digits(), null, emailPendente);
        }

        Proprietario proprietario = principal.proprietarioId() == null ? null : proprietarioRepository.findById(principal.proprietarioId())
            .orElseThrow(() -> new AutenticacaoInvalidaException("usuário autenticado não encontrado"));
        if (proprietario == null) {
            return new MeResponse(null, null, null, principal.tipoConta(), null, emailVerificado, null, null, emailPendente);
        }
        String avatarUrl = avatarUrl(TipoArquivo.AVATAR_PROPRIETARIO, proprietario.id());
        String cpfCnpj = proprietario.cpfCnpj() == null ? null : proprietario.cpfCnpj().digits();
        return new MeResponse(proprietario.id(), proprietario.nome(), proprietario.email().value(), principal.tipoConta(),
            avatarUrl, emailVerificado, cpfCnpj, proprietario.telefone(), emailPendente);
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

    // instância, não static: precisa do Clock injetado pra computar
    // `expirado` — o domínio nunca grava um status "expirado", é sempre
    // derivado de expiraEm vs. "agora" na hora da resposta (ver
    // ConviteAcesso.expirado(Instant) e a mesma decisão em
    // ConvitesController.toResponse para o Convite de locação).
    private ConviteAcessoResponse toResponse(ConviteAcesso convite) {
        return new ConviteAcessoResponse(
            convite.token(),
            convite.email().value(),
            convite.tipo(),
            convite.nome(),
            convite.documento(),
            convite.perfilProprietario(),
            convite.consumido(),
            convite.expirado(clock.now()));
    }

    private static Optional<String> extractToken(HttpRequest<?> request) {
        return request.getHeaders().getAuthorization()
            .filter(h -> h.startsWith("Bearer "))
            .map(h -> h.substring("Bearer ".length()).trim())
            .filter(t -> !t.isBlank());
    }
}
