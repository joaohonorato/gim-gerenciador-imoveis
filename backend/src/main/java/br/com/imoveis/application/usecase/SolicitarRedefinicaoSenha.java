package br.com.imoveis.application.usecase;

import br.com.imoveis.application.ports.Clock;
import br.com.imoveis.application.ports.ContaAcessoRepository;
import br.com.imoveis.application.ports.TokenContaEmailSender;
import br.com.imoveis.application.ports.TokenContaRepository;
import br.com.imoveis.domain.auth.TokenConta;
import br.com.imoveis.domain.shared.Email;
import jakarta.inject.Singleton;

@Singleton
public class SolicitarRedefinicaoSenha {

    private final ContaAcessoRepository contaAcessoRepository;
    private final TokenContaRepository tokenContaRepository;
    private final TokenContaEmailSender emailSender;
    private final Clock clock;

    public SolicitarRedefinicaoSenha(ContaAcessoRepository contaAcessoRepository,
                                     TokenContaRepository tokenContaRepository,
                                     TokenContaEmailSender emailSender,
                                     Clock clock) {
        this.contaAcessoRepository = contaAcessoRepository;
        this.tokenContaRepository = tokenContaRepository;
        this.emailSender = emailSender;
        this.clock = clock;
    }

    /**
     * Não revela se a conta existe — se o e-mail não tiver conta associada,
     * retorna silenciosamente sem criar token nem enviar nada. Evita que o
     * endpoint sirva de oráculo pra enumerar contas cadastradas.
     */
    public void execute(String email) {
        Email destino = new Email(email);
        if (contaAcessoRepository.findByEmail(destino).isEmpty()) {
            return;
        }

        TokenConta token = tokenContaRepository.save(TokenConta.paraResetSenha(destino, clock.now()));
        emailSender.enviarResetSenha(destino.value(), token.token());
    }
}
