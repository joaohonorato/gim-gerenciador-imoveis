package br.com.imoveis.domain.imovel;

import br.com.imoveis.domain.shared.Dinheiro;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public class Conta {

    private final UUID id;
    private final UUID imovelId;
    private final TipoConta tipo;
    private final LocalDate vencimento;
    private final Dinheiro valor;
    private ContaStatus status;

    private Conta(UUID id, UUID imovelId, TipoConta tipo, LocalDate vencimento, Dinheiro valor, ContaStatus status) {
        this.id = id;
        this.imovelId = imovelId;
        this.tipo = tipo;
        this.vencimento = vencimento;
        this.valor = valor;
        this.status = status;
    }

    public static Conta registrar(UUID imovelId, TipoConta tipo, LocalDate vencimento, Dinheiro valor) {
        Objects.requireNonNull(imovelId);
        Objects.requireNonNull(tipo);
        Objects.requireNonNull(vencimento);
        Objects.requireNonNull(valor);
        return new Conta(UUID.randomUUID(), imovelId, tipo, vencimento, valor, ContaStatus.PENDENTE);
    }

    public static Conta reconstituir(UUID id, UUID imovelId, TipoConta tipo, LocalDate vencimento,
                                      Dinheiro valor, ContaStatus status) {
        return new Conta(id, imovelId, tipo, vencimento, valor, status);
    }

    public void marcarPaga() {
        if (status == ContaStatus.PAGO) {
            throw new IllegalStateException("conta já paga");
        }
        status = ContaStatus.PAGO;
    }

    public UUID id() { return id; }
    public UUID imovelId() { return imovelId; }
    public TipoConta tipo() { return tipo; }
    public LocalDate vencimento() { return vencimento; }
    public Dinheiro valor() { return valor; }
    public ContaStatus status() { return status; }
}
