package br.com.imoveis.infrastructure.persistence;

import br.com.imoveis.application.ports.MagicLinkRepository;
import br.com.imoveis.domain.shared.Email;
import br.com.imoveis.infrastructure.persistence.jpa.MagicLinkJpaEntity;
import br.com.imoveis.infrastructure.persistence.jpa.MagicLinkJpaRepository;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Singleton
@Transactional
public class MagicLinkRepositoryAdapter implements MagicLinkRepository {

    private final MagicLinkJpaRepository jpa;
    private final EntityManager em;

    public MagicLinkRepositoryAdapter(MagicLinkJpaRepository jpa, EntityManager em) {
        this.jpa = jpa;
        this.em = em;
    }

    @Override
    public MagicLink save(MagicLink link) {
        MagicLinkJpaEntity e = new MagicLinkJpaEntity();
        e.setId(UUID.randomUUID());
        e.setToken(link.token());
        e.setEmail(link.email().value());
        e.setExpiraEm(link.expiraEm());
        e.setConsumido(link.consumido());
        e.setCriadoEm(Instant.now());
        em.merge(e);
        return link;
    }

    @Override
    public Optional<MagicLink> findByToken(String token) {
        return jpa.findByToken(token).map(this::toDomain);
    }

    @Override
    public Optional<MagicLink> findMostRecentByEmail(Email email) {
        List<MagicLinkJpaEntity> found = jpa.findByEmailOrderByCriadoEmDesc(email.value());
        return found.isEmpty() ? Optional.empty() : Optional.of(toDomain(found.get(0)));
    }

    @Override
    public void consumir(String token) {
        jpa.findByToken(token).ifPresent(e -> {
            e.setConsumido(true);
            em.merge(e);
        });
    }

    private MagicLink toDomain(MagicLinkJpaEntity e) {
        return new MagicLink(e.getToken(), new Email(e.getEmail()), e.getExpiraEm(), e.isConsumido());
    }
}
