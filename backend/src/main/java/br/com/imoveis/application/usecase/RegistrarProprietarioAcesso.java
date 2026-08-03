package br.com.imoveis.application.usecase;

import br.com.imoveis.application.exception.ConflitoException;
import br.com.imoveis.application.ports.Clock;
import br.com.imoveis.application.ports.ContaAcessoRepository;
import br.com.imoveis.application.ports.PasswordHasher;
import br.com.imoveis.application.ports.ProprietarioRepository;
import br.com.imoveis.application.ports.SessaoRepository;
import br.com.imoveis.application.ports.TokenContaEmailSender;
import br.com.imoveis.application.ports.TokenContaRepository;
import br.com.imoveis.domain.auth.ContaAcesso;
import br.com.imoveis.domain.auth.TokenConta;
import br.com.imoveis.domain.proprietario.Proprietario;
import br.com.imoveis.domain.shared.CpfCnpj;
import br.com.imoveis.domain.shared.Email;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;

import java.security.SecureRandom;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Singleton
@Transactional
public class RegistrarProprietarioAcesso {

    private static final SecureRandom RNG = new SecureRandom();

    private final ProprietarioRepository proprietarioRepository;
    private final ContaAcessoRepository contaAcessoRepository;
    private final PasswordHasher passwordHasher;
    private final SessaoRepository sessaoRepository;
    private final TokenContaRepository tokenContaRepository;
    private final TokenContaEmailSender tokenContaEmailSender;
    private final Clock clock;

    public RegistrarProprietarioAcesso(ProprietarioRepository proprietarioRepository,
                                       ContaAcessoRepository contaAcessoRepository,
                                       PasswordHasher passwordHasher,
                                       SessaoRepository sessaoRepository,
                                       TokenContaRepository tokenContaRepository,
                                       TokenContaEmailSender tokenContaEmailSender,
                                       Clock clock) {
        this.proprietarioRepository = proprietarioRepository;
        this.contaAcessoRepository = contaAcessoRepository;
        this.passwordHasher = passwordHasher;
        this.sessaoRepository = sessaoRepository;
        this.tokenContaRepository = tokenContaRepository;
        this.tokenContaEmailSender = tokenContaEmailSender;
        this.clock = clock;
    }

    public Result execute(String email, String senha, String nome, String cpfCnpj) {
        Email ownerEmail = new Email(email);
        CpfCnpj ownerCpfCnpj = CpfCnpj.parse(cpfCnpj);

        if (contaAcessoRepository.findByEmail(ownerEmail).isPresent()) {
            throw new ConflitoException("já existe conta cadastrada para este e-mail");
        }
        if (proprietarioRepository.findByCpfCnpj(ownerCpfCnpj).isPresent()) {
            throw new ConflitoException("já existe proprietário cadastrado para este cpf/cnpj");
        }

        Proprietario proprietario = proprietarioRepository.save(
            Proprietario.cadastrar(nome, ownerCpfCnpj, ownerEmail, clock.now()));
        ContaAcesso contaAcesso = contaAcessoRepository.save(
            ContaAcesso.vincularProprietario(ownerEmail, passwordHasher.hash(senha), proprietario.id(), clock.now()));

        String sessionToken = geraToken();
        sessaoRepository.save(new SessaoRepository.Sessao(
            sessionToken, contaAcesso.id(), contaAcesso.tipo(), contaAcesso.proprietarioId(),
            contaAcesso.inquilinoId(), clock.now().plus(24, ChronoUnit.HOURS)));

        // Não bloqueia nem falha o registro se o envio falhar — mesmo
        // comportamento gracioso do TokenContaEmailSender (log e segue).
        TokenConta tokenVerificacao = tokenContaRepository.save(TokenConta.paraVerificacaoEmail(ownerEmail, clock.now()));
        tokenContaEmailSender.enviarVerificacaoEmail(ownerEmail.value(), tokenVerificacao.token());

        return new Result(sessionToken, proprietario, contaAcesso);
    }

    private static String geraToken() {
        byte[] bytes = new byte[24];
        RNG.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record Result(String sessionToken, Proprietario proprietario, ContaAcesso contaAcesso) {}
}
