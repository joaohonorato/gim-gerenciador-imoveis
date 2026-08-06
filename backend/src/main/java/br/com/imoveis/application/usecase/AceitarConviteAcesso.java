package br.com.imoveis.application.usecase;

import br.com.imoveis.application.exception.AutenticacaoInvalidaException;
import br.com.imoveis.application.exception.ConflitoException;
import br.com.imoveis.application.ports.Clock;
import br.com.imoveis.application.ports.ContaAcessoRepository;
import br.com.imoveis.application.ports.ConviteAcessoRepository;
import br.com.imoveis.application.ports.PasswordHasher;
import br.com.imoveis.application.ports.ProprietarioRepository;
import br.com.imoveis.application.ports.SessaoRepository;
import br.com.imoveis.domain.auditoria.EntidadeAuditoria;
import br.com.imoveis.domain.auditoria.TipoEventoAuditoria;
import br.com.imoveis.domain.auth.ContaAcesso;
import br.com.imoveis.domain.auth.ConviteAcesso;
import br.com.imoveis.domain.proprietario.Proprietario;
import br.com.imoveis.domain.shared.CpfCnpj;
import br.com.imoveis.domain.shared.Email;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;

import java.security.SecureRandom;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Singleton
@Transactional
public class AceitarConviteAcesso {

    private static final SecureRandom RNG = new SecureRandom();

    private final ConviteAcessoRepository conviteAcessoRepository;
    private final ContaAcessoRepository contaAcessoRepository;
    private final ProprietarioRepository proprietarioRepository;
    private final PasswordHasher passwordHasher;
    private final SessaoRepository sessaoRepository;
    private final SemearCatalogosPadrao semearCatalogosPadrao;
    private final RegistrarEventoAuditoria registrarEventoAuditoria;
    private final Clock clock;

    public AceitarConviteAcesso(ConviteAcessoRepository conviteAcessoRepository,
                                ContaAcessoRepository contaAcessoRepository,
                                ProprietarioRepository proprietarioRepository,
                                PasswordHasher passwordHasher,
                                SessaoRepository sessaoRepository,
                                SemearCatalogosPadrao semearCatalogosPadrao,
                                RegistrarEventoAuditoria registrarEventoAuditoria,
                                Clock clock) {
        this.conviteAcessoRepository = conviteAcessoRepository;
        this.contaAcessoRepository = contaAcessoRepository;
        this.proprietarioRepository = proprietarioRepository;
        this.passwordHasher = passwordHasher;
        this.sessaoRepository = sessaoRepository;
        this.semearCatalogosPadrao = semearCatalogosPadrao;
        this.registrarEventoAuditoria = registrarEventoAuditoria;
        this.clock = clock;
    }

    public Result execute(String token, String email, String senha) {
        ConviteAcesso convite = conviteAcessoRepository.findByToken(token)
            .orElseThrow(() -> new AutenticacaoInvalidaException("convite inválido"));
        if (convite.consumido()) {
            registrarEventoAuditoria.execute(EntidadeAuditoria.CONVITE_ACESSO, convite.id(),
                TipoEventoAuditoria.TENTATIVA_COM_TOKEN_INVALIDO_OU_CONSUMIDO, "já consumido");
            throw new AutenticacaoInvalidaException("convite já consumido");
        }
        if (convite.expirado(clock.now())) {
            registrarEventoAuditoria.execute(EntidadeAuditoria.CONVITE_ACESSO, convite.id(),
                TipoEventoAuditoria.TENTATIVA_COM_TOKEN_EXPIRADO, null);
            throw new AutenticacaoInvalidaException("convite expirado");
        }

        Email loginEmail = new Email(email);
        if (!convite.email().equals(loginEmail)) {
            throw new ConflitoException("o e-mail informado não corresponde ao convite");
        }
        if (contaAcessoRepository.findByEmail(loginEmail).isPresent()) {
            throw new ConflitoException("já existe conta cadastrada para este e-mail");
        }

        Optional<Proprietario> proprietarioExistente = proprietarioRepository.findByEmail(loginEmail);
        Proprietario proprietario;
        if (proprietarioExistente.isPresent()) {
            proprietario = proprietarioExistente.get();
        } else {
            proprietario = proprietarioRepository.save(
                Proprietario.reconstituir(
                    UUID.randomUUID(),
                    convite.nome(),
                    CpfCnpj.parse(convite.documento()),
                    loginEmail,
                    convite.perfilProprietario(),
                    clock.now(),
                    null));
            semearCatalogosPadrao.execute(proprietario.id());
        }

        ContaAcesso contaAcesso = contaAcessoRepository.save(
            ContaAcesso.vincularProprietario(loginEmail, passwordHasher.hash(senha), proprietario.id(), clock.now()));
        convite.consumir();
        conviteAcessoRepository.save(convite);
        registrarEventoAuditoria.execute(EntidadeAuditoria.CONVITE_ACESSO, convite.id(), TipoEventoAuditoria.ACEITO, null);

        String sessionToken = geraToken();
        sessaoRepository.save(new SessaoRepository.Sessao(
            sessionToken, contaAcesso.id(), contaAcesso.tipo(), contaAcesso.proprietarioId(),
            contaAcesso.inquilinoId(), clock.now().plus(24, ChronoUnit.HOURS)));
        return new Result(sessionToken, proprietario);
    }

    private static String geraToken() {
        byte[] bytes = new byte[24];
        RNG.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record Result(String sessionToken, Proprietario proprietario) {}
}