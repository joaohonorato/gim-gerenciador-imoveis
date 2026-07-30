package br.com.imoveis.infrastructure.persistence;

import br.com.imoveis.application.ports.ConviteAcessoRepository;
import br.com.imoveis.domain.auth.ConviteAcesso;
import br.com.imoveis.domain.proprietario.PerfilProprietario;
import br.com.imoveis.domain.shared.Email;
import br.com.imoveis.infrastructure.persistence.jpa.ConviteAcessoJpaEntity;
import br.com.imoveis.infrastructure.persistence.jpa.ConviteAcessoJpaRepository;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Singleton
@Transactional
public class ConviteAcessoRepositoryAdapter implements ConviteAcessoRepository {

    private final ConviteAcessoJpaRepository jpa;
    private final EntityManager em;

    public ConviteAcessoRepositoryAdapter(ConviteAcessoJpaRepository jpa, EntityManager em) {
        this.jpa = jpa;
        this.em = em;
    }

    @Override
    public ConviteAcesso save(ConviteAcesso conviteAcesso) {
        ConviteAcessoJpaEntity entity = jpa.findById(conviteAcesso.id()).orElseGet(ConviteAcessoJpaEntity::new);
        boolean novo = entity.getId() == null;
        entity.setId(conviteAcesso.id());
        entity.setToken(conviteAcesso.token());
        entity.setEmail(conviteAcesso.email().value());
        entity.setTipo(conviteAcesso.tipo());
        entity.setNome(conviteAcesso.nome());
        entity.setDocumento(conviteAcesso.documento());
        entity.setPerfilProprietario(conviteAcesso.perfilProprietario() == null ? null : conviteAcesso.perfilProprietario().name());
        entity.setExpiraEm(conviteAcesso.expiraEm());
        entity.setConsumido(conviteAcesso.consumido());
        entity.setCriadoEm(novo ? Instant.now() : entity.getCriadoEm());
        em.merge(entity);
        return conviteAcesso;
    }

    @Override
    public Optional<ConviteAcesso> findByToken(String token) {
        return jpa.findByToken(token).map(this::toDomain);
    }

    @Override
    public Optional<ConviteAcesso> findMostRecentByEmail(Email email) {
        List<ConviteAcessoJpaEntity> found = jpa.findByEmailOrderByCriadoEmDesc(email.value());
        return found.isEmpty() ? Optional.empty() : Optional.of(toDomain(found.get(0)));
    }

    private ConviteAcesso toDomain(ConviteAcessoJpaEntity entity) {
        return ConviteAcesso.reconstituir(
            entity.getId(),
            entity.getToken(),
            new Email(entity.getEmail()),
            entity.getTipo(),
            entity.getNome(),
            entity.getDocumento(),
            entity.getPerfilProprietario() == null ? null : PerfilProprietario.valueOf(entity.getPerfilProprietario()),
            entity.getExpiraEm(),
            entity.isConsumido());
    }
}