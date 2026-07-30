package br.com.imoveis.application.ports;

import java.util.UUID;

public interface AssinaturaProvider {
    void solicitarAssinatura(UUID contratoId, String documento);
    boolean confirmarAssinatura(UUID contratoId, String tokenParte);
}
