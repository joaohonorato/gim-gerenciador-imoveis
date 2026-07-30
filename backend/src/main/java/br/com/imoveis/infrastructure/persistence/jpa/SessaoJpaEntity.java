package br.com.imoveis.infrastructure.persistence.jpa;

import br.com.imoveis.domain.auth.TipoContaAcesso;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sessoes")
public class SessaoJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @Column(name = "conta_acesso_id", nullable = false)
    private UUID contaAcessoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_conta", nullable = false)
    private TipoContaAcesso tipoConta;

    @Column(name = "proprietario_id")
    private UUID proprietarioId;

    @Column(name = "inquilino_id")
    private UUID inquilinoId;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public UUID getContaAcessoId() { return contaAcessoId; }
    public void setContaAcessoId(UUID contaAcessoId) { this.contaAcessoId = contaAcessoId; }

    public TipoContaAcesso getTipoConta() { return tipoConta; }
    public void setTipoConta(TipoContaAcesso tipoConta) { this.tipoConta = tipoConta; }

    public UUID getProprietarioId() { return proprietarioId; }
    public void setProprietarioId(UUID proprietarioId) { this.proprietarioId = proprietarioId; }

    public UUID getInquilinoId() { return inquilinoId; }
    public void setInquilinoId(UUID inquilinoId) { this.inquilinoId = inquilinoId; }

    public Instant getExpiraEm() { return expiraEm; }
    public void setExpiraEm(Instant expiraEm) { this.expiraEm = expiraEm; }
}
