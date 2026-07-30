package br.com.imoveis.application.ports;

import br.com.imoveis.domain.shared.Email;

import java.time.Instant;
import java.util.Optional;

public interface MagicLinkRepository {

    record MagicLink(String token, Email email, Instant expiraEm, boolean consumido) {}

    MagicLink save(MagicLink link);
    Optional<MagicLink> findByToken(String token);
    Optional<MagicLink> findMostRecentByEmail(Email email);
    void consumir(String token);
}
