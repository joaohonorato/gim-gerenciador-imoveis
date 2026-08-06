package br.com.imoveis.application.usecase;

import br.com.imoveis.application.ports.Clock;
import br.com.imoveis.application.ports.PushTokenRepository;
import br.com.imoveis.domain.auth.PushToken;
import jakarta.inject.Singleton;

import java.util.UUID;

@Singleton
public class RegistrarPushToken {

    private final PushTokenRepository pushTokenRepository;
    private final Clock clock;

    public RegistrarPushToken(PushTokenRepository pushTokenRepository, Clock clock) {
        this.pushTokenRepository = pushTokenRepository;
        this.clock = clock;
    }

    public void execute(UUID contaAcessoId, String token) {
        pushTokenRepository.save(PushToken.registrar(contaAcessoId, token, clock.now()));
    }
}
