package br.com.imoveis.application.usecase;

import br.com.imoveis.application.exception.NaoEncontradoException;
import br.com.imoveis.application.ports.Clock;
import br.com.imoveis.application.ports.ContratoRepository;
import br.com.imoveis.domain.contrato.Contrato;
import br.com.imoveis.domain.contrato.Pagamento;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;

import java.util.UUID;

@Singleton
@Transactional
public class ConfirmarPagamento {

    private final ContratoRepository contratoRepository;
    private final Clock clock;

    public ConfirmarPagamento(ContratoRepository contratoRepository, Clock clock) {
        this.contratoRepository = contratoRepository;
        this.clock = clock;
    }

    public Pagamento execute(UUID pagamentoId, UUID proprietarioId) {
        Pagamento pagamento = contratoRepository.findPagamentoById(pagamentoId)
            .orElseThrow(() -> new NaoEncontradoException("pagamento"));
        Contrato contrato = contratoRepository.findById(pagamento.contratoId())
            .orElseThrow(() -> new NaoEncontradoException("contrato"));
        if (!contrato.proprietarioId().equals(proprietarioId)) {
            throw new NaoEncontradoException("pagamento");
        }
        pagamento.confirmar(clock.today());
        return contratoRepository.savePagamento(pagamento);
    }
}
