package br.com.imoveis.application.fakes;

import br.com.imoveis.application.ports.ContratoRepository;
import br.com.imoveis.domain.contrato.Contrato;
import br.com.imoveis.domain.contrato.ContratoAssinaturaStatus;
import br.com.imoveis.domain.contrato.Pagamento;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class FakeContratoRepository implements ContratoRepository {
    public final Map<UUID, Contrato> contratos = new HashMap<>();
    public final Map<UUID, Pagamento> pagamentos = new HashMap<>();

    @Override public Contrato save(Contrato c) { contratos.put(c.id(), c); return c; }
    @Override public Optional<Contrato> findById(UUID id) { return Optional.ofNullable(contratos.get(id)); }
    @Override public List<Contrato> findByInquilinoId(UUID inquilinoId) {
        return contratos.values().stream().filter(c -> c.inquilinoId().equals(inquilinoId)).toList();
    }
    @Override public List<Contrato> findByProprietarioId(UUID proprietarioId) {
        return contratos.values().stream().filter(c -> c.proprietarioId().equals(proprietarioId)).toList();
    }
    @Override public List<Contrato> findByUnidadeAndStatus(UUID unidadeId, ContratoAssinaturaStatus status) {
        return contratos.values().stream()
            .filter(c -> c.unidadeId().equals(unidadeId))
            .filter(c -> c.statusAssinatura() == status)
            .toList();
    }
    @Override public Optional<Contrato> findByUnidadeInquilinoEPeriodo(UUID unidadeId, UUID inquilinoId,
                                                                        LocalDate dataInicio, LocalDate dataFim) {
        return contratos.values().stream()
            .filter(c -> c.unidadeId().equals(unidadeId))
            .filter(c -> c.inquilinoId().equals(inquilinoId))
            .filter(c -> c.periodo().inicio().equals(dataInicio))
            .filter(c -> c.periodo().fim().equals(dataFim))
            .findFirst();
    }
    @Override public List<Contrato> findParaAlertaVencimento(LocalDate hoje, LocalDate limite) {
        return contratos.values().stream()
            .filter(c -> c.statusAssinatura() == ContratoAssinaturaStatus.ASSINADO)
            .filter(c -> !c.alertaVencimentoEnviado())
            .filter(c -> !c.periodo().fim().isBefore(hoje) && !c.periodo().fim().isAfter(limite))
            .toList();
    }
    @Override public List<Contrato> findParaAlertaGarantia(LocalDate hoje, LocalDate limite) {
        return contratos.values().stream()
            .filter(c -> c.garantia() != null)
            .filter(c -> !c.alertaGarantiaEnviado())
            .filter(c -> !c.garantia().vencimento().isBefore(hoje) && !c.garantia().vencimento().isAfter(limite))
            .toList();
    }
    @Override public Pagamento savePagamento(Pagamento p) { pagamentos.put(p.id(), p); return p; }
    @Override public Optional<Pagamento> findPagamentoById(UUID id) { return Optional.ofNullable(pagamentos.get(id)); }
    @Override public List<Pagamento> findPagamentosByContrato(UUID contratoId) {
        return new ArrayList<>(pagamentos.values().stream().filter(p -> p.contratoId().equals(contratoId)).toList());
    }
    @Override public List<Pagamento> findPagamentosByProprietarioId(UUID proprietarioId) {
        List<UUID> contratoIds = contratos.values().stream()
            .filter(c -> c.proprietarioId().equals(proprietarioId))
            .map(Contrato::id)
            .toList();
        return pagamentos.values().stream().filter(p -> contratoIds.contains(p.contratoId())).toList();
    }
}
