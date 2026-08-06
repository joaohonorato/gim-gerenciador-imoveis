package br.com.imoveis.application.ports;

import br.com.imoveis.domain.auth.PushToken;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PushTokenRepository {
    PushToken save(PushToken pushToken);
    Optional<PushToken> findByToken(String token);
    List<PushToken> findByContaAcessoId(UUID contaAcessoId);
}
