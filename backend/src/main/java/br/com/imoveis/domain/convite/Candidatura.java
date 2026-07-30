package br.com.imoveis.domain.convite;

import br.com.imoveis.domain.contrato.GarantiaTipo;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Candidatura {

    private final UUID id;
    private final UUID conviteId;
    private final UUID inquilinoId;
    private final Instant criadaEm;
    private CandidaturaStatus status;
    private GarantiaTipo garantiaEscolhida;
    private String dadosGarantia;

    private Candidatura(UUID id, UUID conviteId, UUID inquilinoId, Instant criadaEm,
                         CandidaturaStatus status, GarantiaTipo garantiaEscolhida, String dadosGarantia) {
        this.id = id;
        this.conviteId = conviteId;
        this.inquilinoId = inquilinoId;
        this.criadaEm = criadaEm;
        this.status = status;
        this.garantiaEscolhida = garantiaEscolhida;
        this.dadosGarantia = dadosGarantia;
    }

    public static Candidatura nova(UUID conviteId, UUID inquilinoId, Instant agora) {
        return new Candidatura(UUID.randomUUID(), conviteId, inquilinoId, agora,
            CandidaturaStatus.PENDENTE, null, null);
    }

    public static Candidatura reconstituir(UUID id, UUID conviteId, UUID inquilinoId, Instant criadaEm,
                                            CandidaturaStatus status, GarantiaTipo garantiaEscolhida,
                                            String dadosGarantia) {
        return new Candidatura(id, conviteId, inquilinoId, criadaEm, status, garantiaEscolhida, dadosGarantia);
    }

    public void definirGarantia(GarantiaTipo tipo, String dados) {
        Objects.requireNonNull(tipo, "tipo obrigatório");
        this.garantiaEscolhida = tipo;
        this.dadosGarantia = dados == null ? "{}" : dados;
    }

    public void aprovar() {
        if (status != CandidaturaStatus.PENDENTE) {
            throw new IllegalStateException("candidatura já processada: " + status);
        }
        status = CandidaturaStatus.APROVADA;
    }

    public void recusar() {
        if (status != CandidaturaStatus.PENDENTE) {
            throw new IllegalStateException("candidatura já processada: " + status);
        }
        status = CandidaturaStatus.RECUSADA;
    }

    public UUID id() { return id; }
    public UUID conviteId() { return conviteId; }
    public UUID inquilinoId() { return inquilinoId; }
    public Instant criadaEm() { return criadaEm; }
    public CandidaturaStatus status() { return status; }
    public GarantiaTipo garantiaEscolhida() { return garantiaEscolhida; }
    public String dadosGarantia() { return dadosGarantia; }
}
