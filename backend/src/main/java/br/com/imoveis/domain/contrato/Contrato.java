package br.com.imoveis.domain.contrato;

import br.com.imoveis.domain.shared.Dinheiro;
import br.com.imoveis.domain.shared.Periodo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Contrato {

    private final UUID id;
    private final UUID unidadeId;
    private final UUID inquilinoId;
    private final UUID proprietarioId;
    private final Periodo periodo;
    private final TipoContrato tipo;
    private final Dinheiro valorAluguel;
    private final String indiceReajuste;
    private ContratoAssinaturaStatus statusAssinatura;
    private final EnumSet<ParteContrato> assinaturas;
    private Garantia garantia;
    private final List<Pagamento> pagamentos;

    private Contrato(UUID id, UUID unidadeId, UUID inquilinoId, UUID proprietarioId, Periodo periodo,
                     TipoContrato tipo, Dinheiro valorAluguel, String indiceReajuste,
                     ContratoAssinaturaStatus status, EnumSet<ParteContrato> assinaturas,
                     Garantia garantia, List<Pagamento> pagamentos) {
        this.id = id;
        this.unidadeId = unidadeId;
        this.inquilinoId = inquilinoId;
        this.proprietarioId = proprietarioId;
        this.periodo = periodo;
        this.tipo = tipo;
        this.valorAluguel = valorAluguel;
        this.indiceReajuste = indiceReajuste;
        this.statusAssinatura = status;
        this.assinaturas = assinaturas;
        this.garantia = garantia;
        this.pagamentos = pagamentos;
    }

    public static Contrato novo(UUID unidadeId, UUID inquilinoId, UUID proprietarioId,
                                 Periodo periodo, TipoContrato tipo, Dinheiro valorAluguel,
                                 String indiceReajuste) {
        Objects.requireNonNull(unidadeId, "unidadeId obrigatório");
        Objects.requireNonNull(inquilinoId, "inquilinoId obrigatório");
        Objects.requireNonNull(proprietarioId, "proprietarioId obrigatório");
        Objects.requireNonNull(periodo, "periodo obrigatório");
        Objects.requireNonNull(tipo, "tipo obrigatório");
        Objects.requireNonNull(valorAluguel, "valorAluguel obrigatório");
        return new Contrato(UUID.randomUUID(), unidadeId, inquilinoId, proprietarioId, periodo,
            tipo, valorAluguel, indiceReajuste == null ? "IPCA" : indiceReajuste,
            ContratoAssinaturaStatus.PENDENTE, EnumSet.noneOf(ParteContrato.class),
            null, new ArrayList<>());
    }

    public static Contrato reconstituir(UUID id, UUID unidadeId, UUID inquilinoId, UUID proprietarioId,
                                         Periodo periodo, TipoContrato tipo, Dinheiro valorAluguel,
                                         String indiceReajuste, ContratoAssinaturaStatus status,
                                         EnumSet<ParteContrato> assinaturas, Garantia garantia,
                                         List<Pagamento> pagamentos) {
        return new Contrato(id, unidadeId, inquilinoId, proprietarioId, periodo, tipo, valorAluguel,
            indiceReajuste, status, EnumSet.copyOf(assinaturas.isEmpty() ? EnumSet.noneOf(ParteContrato.class) : assinaturas),
            garantia, new ArrayList<>(pagamentos));
    }

    public void definirGarantia(GarantiaTipo tipo, LocalDate vencimento, String dadosEspecificos) {
        if (this.garantia != null) {
            throw new IllegalStateException("contrato já possui garantia; não é permitido combinar tipos");
        }
        this.garantia = Garantia.nova(this.id, tipo, vencimento, dadosEspecificos);
    }

    public void assinar(ParteContrato parte) {
        if (statusAssinatura == ContratoAssinaturaStatus.ASSINADO) {
            throw new IllegalStateException("contrato já assinado");
        }
        assinaturas.add(parte);
        if (assinaturas.contains(ParteContrato.PROPRIETARIO) && assinaturas.contains(ParteContrato.INQUILINO)) {
            statusAssinatura = ContratoAssinaturaStatus.ASSINADO;
            gerarPagamentos();
        }
    }

    private void gerarPagamentos() {
        int meses = periodo.meses();
        for (int i = 0; i < meses; i++) {
            LocalDate venc = periodo.inicio().plusMonths(i);
            pagamentos.add(Pagamento.pendente(id, venc, valorAluguel));
        }
    }

    public UUID id() { return id; }
    public UUID unidadeId() { return unidadeId; }
    public UUID inquilinoId() { return inquilinoId; }
    public UUID proprietarioId() { return proprietarioId; }
    public Periodo periodo() { return periodo; }
    public TipoContrato tipo() { return tipo; }
    public Dinheiro valorAluguel() { return valorAluguel; }
    public String indiceReajuste() { return indiceReajuste; }
    public ContratoAssinaturaStatus statusAssinatura() { return statusAssinatura; }
    public EnumSet<ParteContrato> assinaturas() { return EnumSet.copyOf(assinaturas.isEmpty() ? EnumSet.noneOf(ParteContrato.class) : assinaturas); }
    public Garantia garantia() { return garantia; }
    public List<Pagamento> pagamentos() { return Collections.unmodifiableList(pagamentos); }
}
