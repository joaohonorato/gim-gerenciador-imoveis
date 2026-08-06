package br.com.imoveis.infrastructure.persistence;

import br.com.imoveis.application.ports.PushTokenRepository;
import br.com.imoveis.domain.auth.PushToken;
import br.com.imoveis.infrastructure.persistence.jpa.PushTokenJpaEntity;
import br.com.imoveis.infrastructure.persistence.jpa.PushTokenJpaRepository;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Singleton
@Transactional
public class PushTokenRepositoryAdapter implements PushTokenRepository {

    private final PushTokenJpaRepository jpa;
    private final EntityManager em;

    public PushTokenRepositoryAdapter(PushTokenJpaRepository jpa, EntityManager em) {
        this.jpa = jpa;
        this.em = em;
    }

    @Override
    public PushToken save(PushToken pushToken) {
        // Upsert por token (não por id): o mesmo device pode re-registrar o
        // mesmo token várias vezes (novo login, refresh do listener no
        // frontend) — nesses casos queremos atualizar a linha existente
        // (inclusive trocando de conta_acesso_id, se o device mudou de
        // usuário), não criar duplicata e violar a constraint de unicidade.
        PushTokenJpaEntity e = jpa.findByToken(pushToken.token()).orElseGet(PushTokenJpaEntity::new);
        UUID id = e.getId() != null ? e.getId() : pushToken.id();
        e.setId(id);
        e.setContaAcessoId(pushToken.contaAcessoId());
        e.setToken(pushToken.token());
        e.setCriadoEm(pushToken.criadoEm());
        em.merge(e);
        return PushToken.reconstituir(id, pushToken.contaAcessoId(), pushToken.token(), pushToken.criadoEm());
    }

    @Override
    public Optional<PushToken> findByToken(String token) {
        return jpa.findByToken(token).map(this::toDomain);
    }

    @Override
    public List<PushToken> findByContaAcessoId(UUID contaAcessoId) {
        return jpa.findByContaAcessoId(contaAcessoId).stream().map(this::toDomain).toList();
    }

    private PushToken toDomain(PushTokenJpaEntity e) {
        return PushToken.reconstituir(e.getId(), e.getContaAcessoId(), e.getToken(), e.getCriadoEm());
    }
}
