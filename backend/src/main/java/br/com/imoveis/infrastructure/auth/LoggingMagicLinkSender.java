package br.com.imoveis.infrastructure.auth;

import br.com.imoveis.application.ports.MagicLinkSender;
import br.com.imoveis.domain.shared.Email;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class LoggingMagicLinkSender implements MagicLinkSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingMagicLinkSender.class);

    @Override
    public void send(Email destino, String token) {
        log.info("magic-link para {} => {}", destino.value(), token);
    }
}
