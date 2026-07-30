package br.com.imoveis.application.ports;

import br.com.imoveis.domain.shared.Email;

public interface MagicLinkSender {
    void send(Email destino, String token);
}
