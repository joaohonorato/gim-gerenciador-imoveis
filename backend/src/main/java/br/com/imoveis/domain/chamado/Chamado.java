package br.com.imoveis.domain.chamado;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Chamado {

    private final UUID id;
    private final UUID imovelId;
    private final UUID abertoPor;
    private final CategoriaChamado categoria;
    private final String descricao;
    private ChamadoStatus status;
    private final Instant abertoEm;
    private Instant resolvidoEm;

    private Chamado(UUID id, UUID imovelId, UUID abertoPor, CategoriaChamado categoria,
                    String descricao, ChamadoStatus status, Instant abertoEm, Instant resolvidoEm) {
        this.id = id;
        this.imovelId = imovelId;
        this.abertoPor = abertoPor;
        this.categoria = categoria;
        this.descricao = descricao;
        this.status = status;
        this.abertoEm = abertoEm;
        this.resolvidoEm = resolvidoEm;
    }

    public static Chamado abrir(UUID imovelId, UUID inquilinoId, CategoriaChamado categoria,
                                 String descricao, Instant agora) {
        Objects.requireNonNull(imovelId);
        Objects.requireNonNull(inquilinoId);
        Objects.requireNonNull(categoria);
        if (descricao == null || descricao.isBlank()) {
            throw new IllegalArgumentException("descricao obrigatória");
        }
        return new Chamado(UUID.randomUUID(), imovelId, inquilinoId, categoria, descricao.trim(),
            ChamadoStatus.ABERTO, agora, null);
    }

    public static Chamado reconstituir(UUID id, UUID imovelId, UUID abertoPor, CategoriaChamado categoria,
                                        String descricao, ChamadoStatus status, Instant abertoEm, Instant resolvidoEm) {
        return new Chamado(id, imovelId, abertoPor, categoria, descricao, status, abertoEm, resolvidoEm);
    }

    public void iniciarAtendimento() {
        if (status != ChamadoStatus.ABERTO) {
            throw new IllegalStateException("chamado não está ABERTO: " + status);
        }
        status = ChamadoStatus.EM_ANDAMENTO;
    }

    public void resolver(Instant agora) {
        if (status == ChamadoStatus.RESOLVIDO) {
            throw new IllegalStateException("chamado já resolvido");
        }
        status = ChamadoStatus.RESOLVIDO;
        resolvidoEm = agora;
    }

    public UUID id() { return id; }
    public UUID imovelId() { return imovelId; }
    public UUID abertoPor() { return abertoPor; }
    public CategoriaChamado categoria() { return categoria; }
    public String descricao() { return descricao; }
    public ChamadoStatus status() { return status; }
    public Instant abertoEm() { return abertoEm; }
    public Instant resolvidoEm() { return resolvidoEm; }
}
