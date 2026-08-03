package br.com.imoveis.application.usecase;

import br.com.imoveis.application.exception.AutenticacaoInvalidaException;
import br.com.imoveis.application.exception.NaoEncontradoException;
import br.com.imoveis.application.ports.Clock;
import br.com.imoveis.application.ports.ContaAcessoRepository;
import br.com.imoveis.application.ports.TokenContaRepository;
import br.com.imoveis.domain.auth.ContaAcesso;
import br.com.imoveis.domain.auth.TokenConta;
import br.com.imoveis.domain.auth.TokenContaFinalidade;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;

@Singleton
@Transactional
public class ConfirmarVerificacaoEmail {

    private final TokenContaRepository tokenContaRepository;
    private final ContaAcessoRepository contaAcessoRepository;
    private final Clock clock;

    public ConfirmarVerificacaoEmail(TokenContaRepository tokenContaRepository,
                                     ContaAcessoRepository contaAcessoRepository,
                                     Clock clock) {
        this.tokenContaRepository = tokenContaRepository;
        this.contaAcessoRepository = contaAcessoRepository;
        this.clock = clock;
    }

    public void execute(String token) {
        TokenConta tokenConta = tokenContaRepository.findByToken(token)
            .orElseThrow(() -> new AutenticacaoInvalidaException("token inválido"));

        if (tokenConta.finalidade() != TokenContaFinalidade.VERIFICACAO_EMAIL) {
            throw new AutenticacaoInvalidaException("token inválido");
        }
        if (tokenConta.consumido()) {
            throw new AutenticacaoInvalidaException("token já utilizado");
        }
        if (tokenConta.expirado(clock.now())) {
            throw new AutenticacaoInvalidaException("token expirado");
        }

        ContaAcesso contaAcesso = contaAcessoRepository.findByEmail(tokenConta.email())
            .orElseThrow(() -> new NaoEncontradoException("conta"));
        if (!contaAcesso.emailVerificado()) {
            contaAcesso.verificarEmail();
            contaAcessoRepository.save(contaAcesso);
        }

        tokenConta.consumir();
        tokenContaRepository.save(tokenConta);
    }
}
