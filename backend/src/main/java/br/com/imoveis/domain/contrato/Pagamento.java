package br.com.imoveis.domain.contrato;

import br.com.imoveis.domain.shared.Dinheiro;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public class Pagamento {

    private final UUID id;
    private final UUID contratoId;
    private final LocalDate vencimento;
    private final Dinheiro valor;
    private LocalDate pagoEm;
    private PagamentoStatus status;

    public Pagamento(UUID id, UUID contratoId, LocalDate vencimento, Dinheiro valor,
                     LocalDate pagoEm, PagamentoStatus status) {
        this.id = Objects.requireNonNull(id);
        this.contratoId = Objects.requireNonNull(contratoId);
        this.vencimento = Objects.requireNonNull(vencimento);
        this.valor = Objects.requireNonNull(valor);
        this.pagoEm = pagoEm;
        this.status = Objects.requireNonNull(status);
    }

    public static Pagamento pendente(UUID contratoId, LocalDate vencimento, Dinheiro valor) {
        return new Pagamento(UUID.randomUUID(), contratoId, vencimento, valor, null, PagamentoStatus.PENDENTE);
    }

    public void confirmar(LocalDate data) {
        if (status == PagamentoStatus.PAGO) {
            throw new IllegalStateException("pagamento já confirmado");
        }
        this.pagoEm = Objects.requireNonNull(data, "data obrigatória");
        this.status = PagamentoStatus.PAGO;
    }

    public void marcarAtrasadoSe(LocalDate hoje) {
        if (status == PagamentoStatus.PENDENTE && hoje.isAfter(vencimento)) {
            status = PagamentoStatus.ATRASADO;
        }
    }

    public UUID id() { return id; }
    public UUID contratoId() { return contratoId; }
    public LocalDate vencimento() { return vencimento; }
    public Dinheiro valor() { return valor; }
    public LocalDate pagoEm() { return pagoEm; }
    public PagamentoStatus status() { return status; }
}
