package br.com.imoveis.infrastructure.rest.dto;

import br.com.imoveis.domain.chamado.CategoriaChamado;
import br.com.imoveis.domain.chamado.Chamado;
import br.com.imoveis.domain.chamado.ChamadoStatus;
import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;
import java.util.UUID;

public final class ChamadoDtos {
    private ChamadoDtos() {}

    @Serdeable
    public record NovoChamadoRequest(UUID inquilinoId, CategoriaChamado categoria, String descricao) {}

    @Serdeable
    public record AtualizarChamadoRequest(ChamadoStatus status) {}

    @Serdeable
    public record ChamadoResponse(UUID id, UUID imovelId, UUID abertoPor, CategoriaChamado categoria,
                                   String descricao, ChamadoStatus status, Instant abertoEm, Instant resolvidoEm) {
        public static ChamadoResponse from(Chamado c) {
            return new ChamadoResponse(c.id(), c.imovelId(), c.abertoPor(), c.categoria(),
                c.descricao(), c.status(), c.abertoEm(), c.resolvidoEm());
        }
    }
}
