package br.com.imoveis.infrastructure.persistence;

import br.com.imoveis.application.ports.ContaAcessoRepository;
import br.com.imoveis.domain.auth.ContaAcesso;
import br.com.imoveis.domain.shared.Email;
import br.com.imoveis.infrastructure.persistence.jpa.ContaAcessoJpaEntity;
import br.com.imoveis.infrastructure.persistence.jpa.ContaAcessoJpaRepository;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;

import java.util.Optional;

@Singleton
@Transactional
public class ContaAcessoRepositoryAdapter implements ContaAcessoRepository {

    private final ContaAcessoJpaRepository jpa;
    private final EntityManager em;

    public ContaAcessoRepositoryAdapter(ContaAcessoJpaRepository jpa, EntityManager em) {
        this.jpa = jpa;
        this.em = em;
    }

    @Override
    public ContaAcesso save(ContaAcesso contaAcesso) {
        ContaAcessoJpaEntity entity = jpa.findById(contaAcesso.id()).orElseGet(ContaAcessoJpaEntity::new);
        entity.setId(contaAcesso.id());
        entity.setEmail(contaAcesso.email().value());
        entity.setSenhaHash(contaAcesso.senhaHash());
        entity.setTipo(contaAcesso.tipo());
        entity.setProprietarioId(contaAcesso.proprietarioId());
        entity.setInquilinoId(contaAcesso.inquilinoId());
        entity.setCriadoEm(contaAcesso.criadoEm());
        em.merge(entity);
        return contaAcesso;
    }

    @Override
    public Optional<ContaAcesso> findByEmail(Email email) {
        return jpa.findByEmail(email.value()).map(this::toDomain);
    }

    private ContaAcesso toDomain(ContaAcessoJpaEntity entity) {
        return ContaAcesso.reconstituir(
            entity.getId(),
            new Email(entity.getEmail()),
            entity.getSenhaHash(),
            entity.getTipo(),
            entity.getProprietarioId(),
            entity.getInquilinoId(),
            entity.getCriadoEm());
    }
}