package br.com.imoveis.application.usecase;

import br.com.imoveis.application.exception.AutenticacaoInvalidaException;
import br.com.imoveis.application.exception.NaoEncontradoException;
import br.com.imoveis.application.ports.Clock;
import br.com.imoveis.application.ports.ContaAcessoRepository;
import br.com.imoveis.application.ports.PasswordHasher;
import br.com.imoveis.application.ports.TokenContaRepository;
import br.com.imoveis.domain.auth.ContaAcesso;
import br.com.imoveis.domain.auth.TokenConta;
import br.com.imoveis.domain.auth.TokenContaFinalidade;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;

@Singleton
@Transactional
public class RedefinirSenha {

    private final TokenContaRepository tokenContaRepository;
    private final ContaAcessoRepository contaAcessoRepository;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    public RedefinirSenha(TokenContaRepository tokenContaRepository,
                          ContaAcessoRepository contaAcessoRepository,
                          PasswordHasher passwordHasher,
                          Clock clock) {
        this.tokenContaRepository = tokenContaRepository;
        this.contaAcessoRepository = contaAcessoRepository;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
    }

    public void execute(String token, String novaSenha) {
        TokenConta tokenConta = tokenContaRepository.findByToken(token)
            .orElseThrow(() -> new AutenticacaoInvalidaException("token inválido"));

        if (tokenConta.finalidade() != TokenContaFinalidade.RESET_SENHA) {
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
        contaAcesso.redefinirSenha(passwordHasher.hash(novaSenha));
        contaAcessoRepository.save(contaAcesso);

        tokenConta.consumir();
        tokenContaRepository.save(tokenConta);
    }
}
