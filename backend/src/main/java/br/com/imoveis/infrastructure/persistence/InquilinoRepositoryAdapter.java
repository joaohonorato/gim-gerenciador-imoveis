package br.com.imoveis.infrastructure.persistence;

import br.com.imoveis.application.ports.InquilinoRepository;
import br.com.imoveis.domain.inquilino.Inquilino;
import br.com.imoveis.domain.shared.Cpf;
import br.com.imoveis.domain.shared.Email;
import br.com.imoveis.infrastructure.persistence.jpa.InquilinoJpaEntity;
import br.com.imoveis.infrastructure.persistence.jpa.InquilinoJpaRepository;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;

import java.util.Optional;
import java.util.UUID;

@Singleton
@Transactional
public class InquilinoRepositoryAdapter implements InquilinoRepository {

    private final InquilinoJpaRepository jpa;
    private final EntityManager em;

    public InquilinoRepositoryAdapter(InquilinoJpaRepository jpa, EntityManager em) {
        this.jpa = jpa;
        this.em = em;
    }

    @Override
    public Inquilino save(Inquilino i) {
        InquilinoJpaEntity e = jpa.findById(i.id()).orElseGet(InquilinoJpaEntity::new);
        e.setId(i.id());
        e.setNome(i.nome());
        e.setCpf(i.cpf().value());
        e.setEmail(i.email().value());
        e.setCriadoEm(i.criadoEm());
        em.merge(e);
        return i;
    }

    @Override public Optional<Inquilino> findById(UUID id) { return jpa.findById(id).map(this::toDomain); }
    @Override public Optional<Inquilino> findByCpf(Cpf cpf) { return jpa.findByCpf(cpf.value()).map(this::toDomain); }
    @Override public Optional<Inquilino> findByEmail(Email e) { return jpa.findByEmail(e.value()).map(this::toDomain); }

    private Inquilino toDomain(InquilinoJpaEntity e) {
        return Inquilino.reconstituir(e.getId(), e.getNome(), new Cpf(e.getCpf()), new Email(e.getEmail()), e.getCriadoEm());
    }
}
