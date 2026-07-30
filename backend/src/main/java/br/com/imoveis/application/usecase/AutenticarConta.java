package br.com.imoveis.application.usecase;

import br.com.imoveis.application.exception.AutenticacaoInvalidaException;
import br.com.imoveis.application.ports.Clock;
import br.com.imoveis.application.ports.ContaAcessoRepository;
import br.com.imoveis.application.ports.PasswordHasher;
import br.com.imoveis.application.ports.ProprietarioRepository;
import br.com.imoveis.application.ports.SessaoRepository;
import br.com.imoveis.domain.auth.ContaAcesso;
import br.com.imoveis.domain.proprietario.Proprietario;
import br.com.imoveis.domain.shared.Email;
import jakarta.inject.Singleton;

import java.security.SecureRandom;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Singleton
public class AutenticarConta {

    private static final SecureRandom RNG = new SecureRandom();

    private final ContaAcessoRepository contaAcessoRepository;
    private final PasswordHasher passwordHasher;
    private final ProprietarioRepository proprietarioRepository;
    private final SessaoRepository sessaoRepository;
    private final Clock clock;

    public AutenticarConta(ContaAcessoRepository contaAcessoRepository,
                           PasswordHasher passwordHasher,
                           ProprietarioRepository proprietarioRepository,
                           SessaoRepository sessaoRepository,
                           Clock clock) {
        this.contaAcessoRepository = contaAcessoRepository;
        this.passwordHasher = passwordHasher;
        this.proprietarioRepository = proprietarioRepository;
        this.sessaoRepository = sessaoRepository;
        this.clock = clock;
    }

    public Result execute(String email, String senha) {
        ContaAcesso contaAcesso = contaAcessoRepository.findByEmail(new Email(email))
            .orElseThrow(() -> new AutenticacaoInvalidaException("usuário ou senha inválidos"));
        if (!passwordHasher.matches(senha, contaAcesso.senhaHash())) {
            throw new AutenticacaoInvalidaException("usuário ou senha inválidos");
        }

        String sessionToken = geraToken();
        sessaoRepository.save(new SessaoRepository.Sessao(
            sessionToken, contaAcesso.id(), contaAcesso.tipo(), contaAcesso.proprietarioId(),
            contaAcesso.inquilinoId(), clock.now().plus(24, ChronoUnit.HOURS)));

        Proprietario proprietario = contaAcesso.proprietarioId() == null ? null : proprietarioRepository.findById(contaAcesso.proprietarioId())
            .orElseThrow(() -> new AutenticacaoInvalidaException("conta sem proprietário válido"));
        return new Result(sessionToken, contaAcesso, proprietario);
    }

    private static String geraToken() {
        byte[] bytes = new byte[24];
        RNG.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record Result(String sessionToken, ContaAcesso contaAcesso, Proprietario proprietario) {}
}