package br.com.imoveis.infrastructure.persistence.jpa;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tipos_conta")
public class TipoContaJpaEntity {

    @Id
    private UUID id;

    @Column(name = "proprietario_id", nullable = false)
    private UUID proprietarioId;

    @Column(nullable = false)
    private String nome;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getProprietarioId() { return proprietarioId; }
    public void setProprietarioId(UUID proprietarioId) { this.proprietarioId = proprietarioId; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public Instant getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Instant criadoEm) { this.criadoEm = criadoEm; }
}
