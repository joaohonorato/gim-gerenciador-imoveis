package br.com.imoveis.infrastructure.auth;

import br.com.imoveis.application.ports.AssinaturaProvider;
import jakarta.inject.Singleton;

import java.util.UUID;

@Singleton
public class StubAssinaturaProvider implements AssinaturaProvider {

    @Override
    public void solicitarAssinatura(UUID contratoId, String documento) {
    }

    @Override
    public boolean confirmarAssinatura(UUID contratoId, String tokenParte) {
        return true;
    }
}
