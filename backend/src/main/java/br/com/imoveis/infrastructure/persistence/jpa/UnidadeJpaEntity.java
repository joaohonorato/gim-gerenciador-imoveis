package br.com.imoveis.infrastructure.persistence.jpa;

import br.com.imoveis.domain.imovel.UnidadeStatus;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "unidades")
public class UnidadeJpaEntity {

    @Id
    private UUID id;

    @Column(name = "imovel_id", nullable = false)
    private UUID imovelId;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private boolean padrao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UnidadeStatus status;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getImovelId() { return imovelId; }
    public void setImovelId(UUID imovelId) { this.imovelId = imovelId; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public boolean isPadrao() { return padrao; }
    public void setPadrao(boolean padrao) { this.padrao = padrao; }

    public UnidadeStatus getStatus() { return status; }
    public void setStatus(UnidadeStatus status) { this.status = status; }
}
