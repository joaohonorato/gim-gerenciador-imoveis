package br.com.imoveis.application.ports;

import br.com.imoveis.domain.auth.ConviteAcesso;
import br.com.imoveis.domain.shared.Email;

import java.util.Optional;

public interface ConviteAcessoRepository {
    ConviteAcesso save(ConviteAcesso conviteAcesso);
    Optional<ConviteAcesso> findByToken(String token);
    Optional<ConviteAcesso> findMostRecentByEmail(Email email);
}