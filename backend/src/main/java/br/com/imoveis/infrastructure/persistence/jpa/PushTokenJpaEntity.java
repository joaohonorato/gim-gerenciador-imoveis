package br.com.imoveis.infrastructure.persistence.jpa;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "push_tokens")
public class PushTokenJpaEntity {

    @Id
    private UUID id;

    @Column(name = "conta_acesso_id", nullable = false)
    private UUID contaAcessoId;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getContaAcessoId() { return contaAcessoId; }
    public void setContaAcessoId(UUID contaAcessoId) { this.contaAcessoId = contaAcessoId; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Instant getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Instant criadoEm) { this.criadoEm = criadoEm; }
}
