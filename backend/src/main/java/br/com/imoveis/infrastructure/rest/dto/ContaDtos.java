package br.com.imoveis.infrastructure.rest.dto;

import br.com.imoveis.domain.contrato.ParteContrato;
import br.com.imoveis.domain.imovel.Conta;
import br.com.imoveis.domain.imovel.ContaStatus;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public final class ContaDtos {
    private ContaDtos() {}

    @Serdeable
    public record NovaContaRequest(UUID tipoContaId, LocalDate vencimento, BigDecimal valor,
                                    ParteContrato responsavel, UUID contratoId) {}

    @Serdeable
    public record ContaResponse(UUID id, UUID unidadeId, UUID tipoContaId, String tipoContaNome,
                                 LocalDate vencimento, BigDecimal valor, ContaStatus status,
                                 ParteContrato responsavel, UUID contratoId) {
        // tipoContaNome é resolvido fora deste DTO (via TipoContaRepository,
        // no controller) e passado explicitamente — este método não tem
        // acesso a repositório, e o nome não faz parte do agregado Conta
        // (que só guarda o tipoContaId).
        public static ContaResponse from(Conta c, String tipoContaNome) {
            return new ContaResponse(c.id(), c.unidadeId(), c.tipoContaId(), tipoContaNome,
                c.vencimento(), c.valor().valor(), c.status(), c.responsavel(), c.contratoId());
        }
    }

    @Serdeable
    public record TipoContaResponse(UUID id, String nome) {}

    @Serdeable
    public record NovoTipoContaRequest(@NotBlank(message = "nome obrigatório") String nome) {}
}
