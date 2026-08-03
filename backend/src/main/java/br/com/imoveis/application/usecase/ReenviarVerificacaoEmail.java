package br.com.imoveis.application.usecase;

import br.com.imoveis.application.exception.ConflitoException;
import br.com.imoveis.application.exception.NaoEncontradoException;
import br.com.imoveis.application.ports.Clock;
import br.com.imoveis.application.ports.ContaAcessoRepository;
import br.com.imoveis.application.ports.TokenContaEmailSender;
import br.com.imoveis.application.ports.TokenContaRepository;
import br.com.imoveis.domain.auth.ContaAcesso;
import br.com.imoveis.domain.auth.TokenConta;
import jakarta.inject.Singleton;

import java.util.UUID;

@Singleton
public class ReenviarVerificacaoEmail {

    private final ContaAcessoRepository contaAcessoRepository;
    private final TokenContaRepository tokenContaRepository;
    private final TokenContaEmailSender emailSender;
    private final Clock clock;

    public ReenviarVerificacaoEmail(ContaAcessoRepository contaAcessoRepository,
                                    TokenContaRepository tokenContaRepository,
                                    TokenContaEmailSender emailSender,
                                    Clock clock) {
        this.contaAcessoRepository = contaAcessoRepository;
        this.tokenContaRepository = tokenContaRepository;
        this.emailSender = emailSender;
        this.clock = clock;
    }

    public void execute(UUID contaAcessoId) {
        ContaAcesso contaAcesso = contaAcessoRepository.findById(contaAcessoId)
            .orElseThrow(() -> new NaoEncontradoException("conta"));
        if (contaAcesso.emailVerificado()) {
            throw new ConflitoException("e-mail já verificado");
        }

        TokenConta token = tokenContaRepository.save(TokenConta.paraVerificacaoEmail(contaAcesso.email(), clock.now()));
        emailSender.enviarVerificacaoEmail(contaAcesso.email().value(), token.token());
    }
}
