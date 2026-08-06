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
import java.util.List;
import java.util.UUID;

public final class ContratoDtos {
    private ContratoDtos() {}

    @Serdeable
    public record ContratoResponse(UUID id, UUID unidadeId, UUID imovelId, UUID inquilinoId, UUID proprietarioId,
                                    String nomeProprietario, String enderecoImovel,
                                    LocalDate dataInicio, LocalDate dataFim, TipoContrato tipo,
                                    BigDecimal valorAluguel, String indiceReajuste,
                                    ContratoAssinaturaStatus statusAssinatura,
                                    boolean assinouProprietario, boolean assinouInquilino,
                                    GarantiaTipo garantiaTipo) {
        // nomeProprietario/enderecoImovel/imovelId são resolvidos fora deste DTO
        // (via ProprietarioRepository/ImovelRepository, no controller) e passados
        // explicitamente — este método não tem acesso a repositório, e nenhum dos
        // três dados faz parte do agregado Contrato (que só guarda unidadeId).
        // imovelId é exposto pro frontend do inquilino conseguir abrir chamado
        // (POST /imoveis/{imovelId}/chamados) sem precisar resolver unidade→imóvel.
        public static ContratoResponse from(Contrato c, String nomeProprietario, String enderecoImovel, UUID imovelId) {
            return new ContratoResponse(c.id(), c.unidadeId(), imovelId, c.inquilinoId(), c.proprietarioId(),
                nomeProprietario, enderecoImovel,
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

    @Serdeable
    public record ArquivoInfoResponse(UUID id, String nomeOriginal) {}

    @Serdeable
    public record DocumentosContratoResponse(ArquivoInfoResponse documentoContrato,
                                              List<ArquivoInfoResponse> documentosGarantia) {}
}
