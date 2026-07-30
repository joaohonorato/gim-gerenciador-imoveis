package br.com.imoveis.infrastructure.persistence.jpa;

import br.com.imoveis.domain.imovel.ContaStatus;
import br.com.imoveis.domain.imovel.TipoConta;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "contas")
public class ContaJpaEntity {

    @Id
    private UUID id;

    @Column(name = "imovel_id", nullable = false)
    private UUID imovelId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoConta tipo;

    @Column(nullable = false)
    private LocalDate vencimento;

    @Column(nullable = false)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContaStatus status;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getImovelId() { return imovelId; }
    public void setImovelId(UUID imovelId) { this.imovelId = imovelId; }

    public TipoConta getTipo() { return tipo; }
    public void setTipo(TipoConta tipo) { this.tipo = tipo; }

    public LocalDate getVencimento() { return vencimento; }
    public void setVencimento(LocalDate vencimento) { this.vencimento = vencimento; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    public ContaStatus getStatus() { return status; }
    public void setStatus(ContaStatus status) { this.status = status; }
}
