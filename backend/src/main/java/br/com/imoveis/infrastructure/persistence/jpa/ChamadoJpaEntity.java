package br.com.imoveis.infrastructure.persistence.jpa;

import br.com.imoveis.domain.chamado.CategoriaChamado;
import br.com.imoveis.domain.chamado.ChamadoStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chamados")
public class ChamadoJpaEntity {

    @Id
    private UUID id;

    @Column(name = "imovel_id", nullable = false)
    private UUID imovelId;

    @Column(name = "aberto_por", nullable = false)
    private UUID abertoPor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoriaChamado categoria;

    @Column(nullable = false, length = 4000)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChamadoStatus status;

    @Column(name = "aberto_em", nullable = false)
    private Instant abertoEm;

    @Column(name = "resolvido_em")
    private Instant resolvidoEm;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getImovelId() { return imovelId; }
    public void setImovelId(UUID imovelId) { this.imovelId = imovelId; }

    public UUID getAbertoPor() { return abertoPor; }
    public void setAbertoPor(UUID abertoPor) { this.abertoPor = abertoPor; }

    public CategoriaChamado getCategoria() { return categoria; }
    public void setCategoria(CategoriaChamado categoria) { this.categoria = categoria; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public ChamadoStatus getStatus() { return status; }
    public void setStatus(ChamadoStatus status) { this.status = status; }

    public Instant getAbertoEm() { return abertoEm; }
    public void setAbertoEm(Instant abertoEm) { this.abertoEm = abertoEm; }

    public Instant getResolvidoEm() { return resolvidoEm; }
    public void setResolvidoEm(Instant resolvidoEm) { this.resolvidoEm = resolvidoEm; }
}
