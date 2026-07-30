package br.com.imoveis.infrastructure.rest.dto;

import br.com.imoveis.domain.imovel.Conta;
import br.com.imoveis.domain.imovel.ContaStatus;
import br.com.imoveis.domain.imovel.TipoConta;
import io.micronaut.serde.annotation.Serdeable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public final class ContaDtos {
    private ContaDtos() {}

    @Serdeable
    public record NovaContaRequest(TipoConta tipo, LocalDate vencimento, BigDecimal valor) {}

    @Serdeable
    public record ContaResponse(UUID id, UUID imovelId, TipoConta tipo, LocalDate vencimento,
                                 BigDecimal valor, ContaStatus status) {
        public static ContaResponse from(Conta c) {
            return new ContaResponse(c.id(), c.imovelId(), c.tipo(), c.vencimento(),
                c.valor().valor(), c.status());
        }
    }
}
