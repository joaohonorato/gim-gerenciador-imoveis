package br.com.imoveis.application.ports;

import br.com.imoveis.domain.auth.TokenConta;
import br.com.imoveis.domain.auth.TokenContaFinalidade;
import br.com.imoveis.domain.shared.Email;

import java.util.Optional;

public interface TokenContaRepository {
    TokenConta save(TokenConta tokenConta);
    Optional<TokenConta> findByToken(String token);
    Optional<TokenConta> findMostRecentByEmailAndFinalidade(Email email, TokenContaFinalidade finalidade);
}
