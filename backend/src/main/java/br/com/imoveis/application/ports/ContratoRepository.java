package br.com.imoveis.application.ports;

import br.com.imoveis.domain.contrato.Contrato;
import br.com.imoveis.domain.contrato.ContratoAssinaturaStatus;
import br.com.imoveis.domain.contrato.Pagamento;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContratoRepository {
    Contrato save(Contrato contrato);
    Optional<Contrato> findById(UUID id);
    List<Contrato> findByInquilinoId(UUID inquilinoId);
    List<Contrato> findByProprietarioId(UUID proprietarioId);
    List<Contrato> findByUnidadeAndStatus(UUID unidadeId, ContratoAssinaturaStatus status);
    Optional<Contrato> findByUnidadeInquilinoEPeriodo(UUID unidadeId, UUID inquilinoId,
                                                      LocalDate dataInicio, LocalDate dataFim);

    Pagamento savePagamento(Pagamento pagamento);
    Optional<Pagamento> findPagamentoById(UUID id);
    List<Pagamento> findPagamentosByContrato(UUID contratoId);
    List<Pagamento> findPagamentosByProprietarioId(UUID proprietarioId);
}
