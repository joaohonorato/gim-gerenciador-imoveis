package br.com.imoveis.infrastructure.rest.dto;

import br.com.imoveis.domain.contrato.Contrato;
import br.com.imoveis.domain.contrato.ContratoAssinaturaStatus;
import br.com.imoveis.domain.contrato.GarantiaTipo;
import br.com.imoveis.domain.contrato.Pagamento;
import br.com.imoveis.domain.contrato.PagamentoStatus;
import br.com.imoveis.domain.contrato.ParteContrato;
import br.com.imoveis.domain.contrato.TipoContrato;
import io.micronaut.serde.annotation.Serdeable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public final class ContratoDtos {
    private ContratoDtos() {}

    @Serdeable
    public record ContratoResponse(UUID id, UUID unidadeId, UUID inquilinoId, UUID proprietarioId,
                                    LocalDate dataInicio, LocalDate dataFim, TipoContrato tipo,
                                    BigDecimal valorAluguel, String indiceReajuste,
                                    ContratoAssinaturaStatus statusAssinatura,
                                    boolean assinouProprietario, boolean assinouInquilino,
                                    GarantiaTipo garantiaTipo) {
        public static ContratoResponse from(Contrato c) {
            return new ContratoResponse(c.id(), c.unidadeId(), c.inquilinoId(), c.proprietarioId(),
                c.periodo().inicio(), c.periodo().fim(), c.tipo(), c.valorAluguel().valor(),
                c.indiceReajuste(), c.statusAssinatura(),
                c.assinaturas().contains(ParteContrato.PROPRIETARIO),
                c.assinaturas().contains(ParteContrato.INQUILINO),
                c.garantia() == null ? null : c.garantia().tipo());
        }
    }

    @Serdeable
    public record AssinarRequest(ParteContrato parte) {}

    @Serdeable
    public record PagamentoResponse(UUID id, UUID contratoId, LocalDate vencimento,
                                     BigDecimal valor, LocalDate pagoEm, PagamentoStatus status) {
        public static PagamentoResponse from(Pagamento p) {
            return new PagamentoResponse(p.id(), p.contratoId(), p.vencimento(),
                p.valor().valor(), p.pagoEm(), p.status());
        }
    }
}
