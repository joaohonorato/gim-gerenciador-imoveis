package br.com.imoveis.infrastructure.rest.dto;

import br.com.imoveis.domain.chamado.Chamado;
import br.com.imoveis.domain.chamado.ChamadoStatus;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;

public final class ChamadoDtos {
    private ChamadoDtos() {}

    @Serdeable
    public record NovoChamadoRequest(UUID categoriaId, String descricao) {}

    @Serdeable
    public record AtualizarChamadoRequest(ChamadoStatus status) {}

    @Serdeable
    public record ChamadoResponse(UUID id, UUID imovelId, UUID unidadeId, String unidadeNome,
                                   UUID abertoPor, UUID categoriaId, String categoriaNome,
                                   String descricao, ChamadoStatus status, Instant abertoEm, Instant resolvidoEm) {
        // categoriaNome/unidadeNome são resolvidos fora deste DTO (via
        // CategoriaChamadoRepository/ImovelRepository, no controller) e
        // passados explicitamente — mesmo padrão de ContaResponse.from(Conta, tipoContaNome).
        public static ChamadoResponse from(Chamado c, String categoriaNome, String unidadeNome) {
            return new ChamadoResponse(c.id(), c.imovelId(), c.unidadeId(), unidadeNome, c.abertoPor(),
                c.categoriaId(), categoriaNome, c.descricao(), c.status(), c.abertoEm(), c.resolvidoEm());
        }
    }

    @Serdeable
    public record CategoriaChamadoResponse(UUID id, String nome) {}

    @Serdeable
    public record NovaCategoriaChamadoRequest(@NotBlank(message = "nome obrigatório") String nome) {}
}
