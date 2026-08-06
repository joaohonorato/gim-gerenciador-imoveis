package br.com.imoveis.application.usecase;

import br.com.imoveis.application.exception.AutenticacaoInvalidaException;
import br.com.imoveis.application.exception.NaoEncontradoException;
import br.com.imoveis.application.ports.Clock;
import br.com.imoveis.application.ports.ContaAcessoRepository;
import br.com.imoveis.application.ports.InquilinoRepository;
import br.com.imoveis.application.ports.ProprietarioRepository;
import br.com.imoveis.application.ports.TokenContaRepository;
import br.com.imoveis.domain.auth.ContaAcesso;
import br.com.imoveis.domain.auth.TokenConta;
import br.com.imoveis.domain.auth.TokenContaFinalidade;
import br.com.imoveis.domain.inquilino.Inquilino;
import br.com.imoveis.domain.proprietario.Proprietario;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;

@Singleton
@Transactional
public class ConfirmarVerificacaoEmail {

    private final TokenContaRepository tokenContaRepository;
    private final ContaAcessoRepository contaAcessoRepository;
    private final ProprietarioRepository proprietarioRepository;
    private final InquilinoRepository inquilinoRepository;
    private final Clock clock;

    public ConfirmarVerificacaoEmail(TokenContaRepository tokenContaRepository,
                                     ContaAcessoRepository contaAcessoRepository,
                                     ProprietarioRepository proprietarioRepository,
                                     InquilinoRepository inquilinoRepository,
                                     Clock clock) {
        this.tokenContaRepository = tokenContaRepository;
        this.contaAcessoRepository = contaAcessoRepository;
        this.proprietarioRepository = proprietarioRepository;
        this.inquilinoRepository = inquilinoRepository;
        this.clock = clock;
    }

    public void execute(String token) {
        TokenConta tokenConta = tokenContaRepository.findByToken(token)
            .orElseThrow(() -> new AutenticacaoInvalidaException("token inválido"));

        if (tokenConta.finalidade() != TokenContaFinalidade.VERIFICACAO_EMAIL
            && tokenConta.finalidade() != TokenContaFinalidade.ALTERACAO_EMAIL) {
            throw new AutenticacaoInvalidaException("token inválido");
        }
        if (tokenConta.consumido()) {
            throw new AutenticacaoInvalidaException("token já utilizado");
        }
        if (tokenConta.expirado(clock.now())) {
            throw new AutenticacaoInvalidaException("token expirado");
        }

        if (tokenConta.finalidade() == TokenContaFinalidade.VERIFICACAO_EMAIL) {
            confirmarVerificacaoInicial(tokenConta);
        } else {
            confirmarAlteracaoEmail(tokenConta);
        }

        tokenConta.consumir();
        tokenContaRepository.save(tokenConta);
    }

    private void confirmarVerificacaoInicial(TokenConta tokenConta) {
        ContaAcesso contaAcesso = contaAcessoRepository.findByEmail(tokenConta.email())
            .orElseThrow(() -> new NaoEncontradoException("conta"));
        if (!contaAcesso.emailVerificado()) {
            contaAcesso.verificarEmail();
            contaAcessoRepository.save(contaAcesso);
        }
    }

    // Diferente da verificação inicial: o e-mail do token ainda não é o
    // email() ativo da conta, é o emailPendente (ver
    // ContaAcesso.solicitarAlteracaoEmail) — por isso o lookup é por
    // findByEmailPendente, não findByEmail.
    private void confirmarAlteracaoEmail(TokenConta tokenConta) {
        ContaAcesso contaAcesso = contaAcessoRepository.findByEmailPendente(tokenConta.email())
            .orElseThrow(() -> new NaoEncontradoException("conta"));
        contaAcesso.confirmarAlteracaoEmail();
        contaAcessoRepository.save(contaAcesso);

        if (contaAcesso.proprietarioId() != null) {
            Proprietario proprietario = proprietarioRepository.findById(contaAcesso.proprietarioId())
                .orElseThrow(() -> new NaoEncontradoException("proprietário"));
            proprietario.atualizarEmail(contaAcesso.email());
            proprietarioRepository.save(proprietario);
        } else if (contaAcesso.inquilinoId() != null) {
            Inquilino inquilino = inquilinoRepository.findById(contaAcesso.inquilinoId())
                .orElseThrow(() -> new NaoEncontradoException("inquilino"));
            inquilino.atualizarEmail(contaAcesso.email());
            inquilinoRepository.save(inquilino);
        }
    }
}
