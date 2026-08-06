package br.com.imoveis.application.usecase;

import br.com.imoveis.application.exception.AutenticacaoInvalidaException;
import br.com.imoveis.application.exception.NaoEncontradoException;
import br.com.imoveis.application.ports.ContaAcessoRepository;
import br.com.imoveis.application.ports.PasswordHasher;
import br.com.imoveis.domain.auth.ContaAcesso;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;

import java.util.UUID;

// Diferente de RedefinirSenha (que parte de um token de e-mail, pra quem
// está deslogado e esqueceu a senha): aqui o usuário já está autenticado e
// precisa provar que conhece a senha atual antes de trocar — sem isso,
// qualquer sessão ativa roubada poderia trocar a senha e travar o dono de
// fora da própria conta.
@Singleton
@Transactional
public class TrocarSenha {

    private final ContaAcessoRepository contaAcessoRepository;
    private final PasswordHasher passwordHasher;

    public TrocarSenha(ContaAcessoRepository contaAcessoRepository, PasswordHasher passwordHasher) {
        this.contaAcessoRepository = contaAcessoRepository;
        this.passwordHasher = passwordHasher;
    }

    public void execute(UUID contaAcessoId, String senhaAtual, String novaSenha) {
        ContaAcesso contaAcesso = contaAcessoRepository.findById(contaAcessoId)
            .orElseThrow(() -> new NaoEncontradoException("conta"));

        if (!passwordHasher.matches(senhaAtual, contaAcesso.senhaHash())) {
            throw new AutenticacaoInvalidaException("senha atual incorreta");
        }

        contaAcesso.redefinirSenha(passwordHasher.hash(novaSenha));
        contaAcessoRepository.save(contaAcesso);
    }
}
