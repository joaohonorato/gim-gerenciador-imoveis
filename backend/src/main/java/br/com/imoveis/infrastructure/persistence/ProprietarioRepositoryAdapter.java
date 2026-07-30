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
        e.setCpfCnpj(p.cpfCnpj().digits());
        e.setEmail(p.email().value());
        e.setPerfil(p.perfil().name());
        e.setCriadoEm(p.criadoEm());
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
        return Proprietario.reconstituir(e.getId(), e.getNome(), CpfCnpj.parse(e.getCpfCnpj()),
            new Email(e.getEmail()), PerfilProprietario.valueOf(e.getPerfil()), e.getCriadoEm());
    }
}
