package br.com.imoveis.application.usecase;

import br.com.imoveis.application.exception.ConflitoException;
import br.com.imoveis.application.exception.NaoEncontradoException;
import br.com.imoveis.application.ports.Clock;
import br.com.imoveis.application.ports.ContaAcessoRepository;
import br.com.imoveis.application.ports.TokenContaEmailSender;
import br.com.imoveis.application.ports.TokenContaRepository;
import br.com.imoveis.domain.auth.ContaAcesso;
import br.com.imoveis.domain.auth.TokenConta;
import br.com.imoveis.domain.shared.Email;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;

import java.util.UUID;

// Autenticado, provando posse da conta atual — diferente de
// RegistrarProprietarioAcesso (cadastro novo) e RedefinirSenha (deslogado,
// via token). Não troca o e-mail de login na hora: guarda como
// emailPendente e só aplica quando o token enviado pro *novo* endereço for
// confirmado (ver ContaAcesso.solicitarAlteracaoEmail / ConfirmarVerificacaoEmail).
@Singleton
@Transactional
public class SolicitarAlteracaoEmail {

    private final ContaAcessoRepository contaAcessoRepository;
    private final TokenContaRepository tokenContaRepository;
    private final TokenContaEmailSender emailSender;
    private final Clock clock;

    public SolicitarAlteracaoEmail(ContaAcessoRepository contaAcessoRepository,
                                   TokenContaRepository tokenContaRepository,
                                   TokenContaEmailSender emailSender,
                                   Clock clock) {
        this.contaAcessoRepository = contaAcessoRepository;
        this.tokenContaRepository = tokenContaRepository;
        this.emailSender = emailSender;
        this.clock = clock;
    }

    public void execute(UUID contaAcessoId, String novoEmailRaw) {
        Email novoEmail = new Email(novoEmailRaw);
        ContaAcesso contaAcesso = contaAcessoRepository.findById(contaAcessoId)
            .orElseThrow(() -> new NaoEncontradoException("conta"));

        if (contaAcessoRepository.findByEmail(novoEmail).isPresent()) {
            throw new ConflitoException("já existe conta cadastrada para este e-mail");
        }

        contaAcesso.solicitarAlteracaoEmail(novoEmail);
        contaAcessoRepository.save(contaAcesso);

        TokenConta token = tokenContaRepository.save(TokenConta.paraAlteracaoEmail(novoEmail, clock.now()));
        emailSender.enviarConfirmacaoAlteracaoEmail(novoEmail.value(), token.token());
    }
}
