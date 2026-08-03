package br.com.imoveis.infrastructure.persistence.jpa;

import br.com.imoveis.domain.auth.TipoContaAcesso;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "contas_acesso")
public class ContaAcessoJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "senha_hash", nullable = false, length = 255)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoContaAcesso tipo;

    @Column(name = "proprietario_id")
    private UUID proprietarioId;

    @Column(name = "inquilino_id")
    private UUID inquilinoId;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "email_verificado", nullable = false)
    private boolean emailVerificado;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }

    public TipoContaAcesso getTipo() { return tipo; }
    public void setTipo(TipoContaAcesso tipo) { this.tipo = tipo; }

    public UUID getProprietarioId() { return proprietarioId; }
    public void setProprietarioId(UUID proprietarioId) { this.proprietarioId = proprietarioId; }

    public UUID getInquilinoId() { return inquilinoId; }
    public void setInquilinoId(UUID inquilinoId) { this.inquilinoId = inquilinoId; }

    public Instant getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Instant criadoEm) { this.criadoEm = criadoEm; }

    public boolean isEmailVerificado() { return emailVerificado; }
    public void setEmailVerificado(boolean emailVerificado) { this.emailVerificado = emailVerificado; }
}