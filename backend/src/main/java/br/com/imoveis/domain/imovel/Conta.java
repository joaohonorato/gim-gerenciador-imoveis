package br.com.imoveis.domain.imovel;

import br.com.imoveis.domain.contrato.ParteContrato;
import br.com.imoveis.domain.shared.Dinheiro;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public class Conta {

    private final UUID id;
    private final UUID unidadeId;
    private final UUID tipoContaId;
    private final LocalDate vencimento;
    private final Dinheiro valor;
    private ContaStatus status;
    private final ParteContrato responsavel;
    private final UUID contratoId;
    private boolean alertaEnviado;

    private Conta(UUID id, UUID unidadeId, UUID tipoContaId, LocalDate vencimento, Dinheiro valor,
                   ContaStatus status, ParteContrato responsavel, UUID contratoId, boolean alertaEnviado) {
        this.id = id;
        this.unidadeId = unidadeId;
        this.tipoContaId = tipoContaId;
        this.vencimento = vencimento;
        this.valor = valor;
        this.status = status;
        this.responsavel = responsavel;
        this.contratoId = contratoId;
        this.alertaEnviado = alertaEnviado;
    }

    // contratoId é obrigatório quando responsavel = INQUILINO (é o que
    // determina de quem é "quanto ele paga esse mês" a conta entra na
    // soma) e proibido quando responsavel = PROPRIETARIO (despesa própria
    // do proprietário, sem inquilino envolvido).
    public static Conta registrar(UUID unidadeId, UUID tipoContaId, LocalDate vencimento, Dinheiro valor,
                                   ParteContrato responsavel, UUID contratoId) {
        Objects.requireNonNull(unidadeId, "unidadeId obrigatório");
        Objects.requireNonNull(tipoContaId, "tipoContaId obrigatório");
        Objects.requireNonNull(vencimento, "vencimento obrigatório");
        Objects.requireNonNull(valor, "valor obrigatório");
        Objects.requireNonNull(responsavel, "responsavel obrigatório");
        validarContratoId(responsavel, contratoId);
        return new Conta(UUID.randomUUID(), unidadeId, tipoContaId, vencimento, valor, ContaStatus.PENDENTE,
            responsavel, contratoId, false);
    }

    public static Conta reconstituir(UUID id, UUID unidadeId, UUID tipoContaId, LocalDate vencimento,
                                      Dinheiro valor, ContaStatus status, ParteContrato responsavel, UUID contratoId,
                                      boolean alertaEnviado) {
        return new Conta(id, unidadeId, tipoContaId, vencimento, valor, status, responsavel, contratoId, alertaEnviado);
    }

    private static void validarContratoId(ParteContrato responsavel, UUID contratoId) {
        if (responsavel == ParteContrato.INQUILINO && contratoId == null) {
            throw new IllegalArgumentException("contratoId obrigatório quando responsavel é INQUILINO");
        }
        if (responsavel == ParteContrato.PROPRIETARIO && contratoId != null) {
            throw new IllegalArgumentException("contratoId não se aplica quando responsavel é PROPRIETARIO");
        }
    }

    public void marcarPaga() {
        if (status == ContaStatus.PAGO) {
            throw new IllegalStateException("conta já paga");
        }
        status = ContaStatus.PAGO;
    }

    // Marcada pelo job diário de alertas de vencimento (ver
    // application/usecase/NotificarContasProximasDoVencimento) depois de
    // notificar com sucesso — evita reenvio duplicado em execuções seguintes.
    public void marcarAlertaEnviado() {
        this.alertaEnviado = true;
    }

    public UUID id() { return id; }
    public UUID unidadeId() { return unidadeId; }
    public UUID tipoContaId() { return tipoContaId; }
    public LocalDate vencimento() { return vencimento; }
    public Dinheiro valor() { return valor; }
    public ContaStatus status() { return status; }
    public ParteContrato responsavel() { return responsavel; }
    public UUID contratoId() { return contratoId; }
    public boolean alertaEnviado() { return alertaEnviado; }
}
