package br.com.imoveis.domain.contrato;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public class Garantia {

    private final UUID id;
    private final UUID contratoId;
    private final GarantiaTipo tipo;
    private final LocalDate vencimento;
    private final String dadosEspecificos;

    public Garantia(UUID id, UUID contratoId, GarantiaTipo tipo, LocalDate vencimento, String dadosEspecificos) {
        this.id = Objects.requireNonNull(id);
        this.contratoId = Objects.requireNonNull(contratoId);
        this.tipo = Objects.requireNonNull(tipo, "tipo obrigatório");
        this.vencimento = Objects.requireNonNull(vencimento, "vencimento obrigatório");
        this.dadosEspecificos = dadosEspecificos == null ? "{}" : dadosEspecificos;
    }

    public static Garantia nova(UUID contratoId, GarantiaTipo tipo, LocalDate vencimento, String dadosEspecificos) {
        return new Garantia(UUID.randomUUID(), contratoId, tipo, vencimento, dadosEspecificos);
    }

    public static Garantia reconstituir(UUID id, UUID contratoId, GarantiaTipo tipo,
                                         LocalDate vencimento, String dadosEspecificos) {
        return new Garantia(id, contratoId, tipo, vencimento, dadosEspecificos);
    }

    public UUID id() { return id; }
    public UUID contratoId() { return contratoId; }
    public GarantiaTipo tipo() { return tipo; }
    public LocalDate vencimento() { return vencimento; }
    public String dadosEspecificos() { return dadosEspecificos; }
}
