package br.com.imoveis.infrastructure.persistence;

import br.com.imoveis.application.ports.ProprietarioRepository;
import br.com.imoveis.domain.proprietario.PerfilProprietario;
import br.com.imoveis.domain.proprietario.Proprietario;
import br.com.imoveis.domain.shared.CpfCnpj;
import br.com.imoveis.domain.shared.Email;
import br.com.imoveis.infrastructure.persistence.jpa.ProprietarioJpaEntity;
import br.com.imoveis.infrastructure.persistence.jpa.ProprietarioJpaRepository;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;

import java.util.Optional;
import java.util.UUID;

@Singleton
@Transactional
public class ProprietarioRepositoryAdapter implements ProprietarioRepository {

    private final ProprietarioJpaRepository jpa;
    private final EntityManager em;

    public ProprietarioRepositoryAdapter(ProprietarioJpaRepository jpa, EntityManager em) {
        this.jpa = jpa;
        this.em = em;
    }

    @Override
    public Proprietario save(Proprietario p) {
        ProprietarioJpaEntity e = jpa.findById(p.id()).orElseGet(ProprietarioJpaEntity::new);
        e.setId(p.id());
        e.setNome(p.nome());
        e.setCpfCnpj(p.cpfCnpj() != null ? p.cpfCnpj().digits() : null);
        e.setEmail(p.email().value());
        e.setPerfil(p.perfil().name());
        e.setCriadoEm(p.criadoEm());
        e.setTelefone(p.telefone());
        em.merge(e);
        return p;
    }

    @Override
    public Optional<Proprietario> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Proprietario> findByEmail(Email email) {
        return jpa.findByEmail(email.value()).map(this::toDomain);
    }

    @Override
    public Optional<Proprietario> findByCpfCnpj(CpfCnpj cpfCnpj) {
        return jpa.findByCpfCnpj(cpfCnpj.digits()).map(this::toDomain);
    }

    private Proprietario toDomain(ProprietarioJpaEntity e) {
        CpfCnpj cpfCnpj = e.getCpfCnpj() != null ? CpfCnpj.parse(e.getCpfCnpj()) : null;
        return Proprietario.reconstituir(e.getId(), e.getNome(), cpfCnpj,
            new Email(e.getEmail()), PerfilProprietario.valueOf(e.getPerfil()), e.getCriadoEm(), e.getTelefone());
    }
}
