package br.com.imoveis.domain.convite;

import br.com.imoveis.domain.contrato.GarantiaTipo;
import br.com.imoveis.domain.contrato.TipoContrato;
import br.com.imoveis.domain.shared.Dinheiro;
import br.com.imoveis.domain.shared.Periodo;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

public class Convite {

    private static final SecureRandom RNG = new SecureRandom();

    private final UUID id;
    private final UUID imovelId;
    private final UUID unidadeId;
    private final UUID proprietarioId;
    private final String token;
    private final Instant expiraEm;
    private final CondicoesConvite condicoes;
    private ConviteStatus status;
    private UUID candidaturaId;
    private CanalEnvioConvite ultimoCanalEnvio;
    private String ultimoStatusEnvio;
    private String ultimoDestinoEnvio;
    private String ultimoWhatsappShareUrl;
    private String ultimoDetalheEnvio;
    private Instant ultimoEnvioEm;
    private int tentativasEnvio;

    private Convite(UUID id, UUID imovelId, UUID unidadeId, UUID proprietarioId,
                    String token, Instant expiraEm, CondicoesConvite condicoes,
                    ConviteStatus status, UUID candidaturaId,
                    CanalEnvioConvite ultimoCanalEnvio,
                    String ultimoStatusEnvio,
                    String ultimoDestinoEnvio,
                    String ultimoWhatsappShareUrl,
                    String ultimoDetalheEnvio,
                    Instant ultimoEnvioEm,
                    int tentativasEnvio) {
        this.id = id;
        this.imovelId = imovelId;
        this.unidadeId = unidadeId;
        this.proprietarioId = proprietarioId;
        this.token = token;
        this.expiraEm = expiraEm;
        this.condicoes = condicoes;
        this.status = status;
        this.candidaturaId = candidaturaId;
        this.ultimoCanalEnvio = ultimoCanalEnvio;
        this.ultimoStatusEnvio = ultimoStatusEnvio;
        this.ultimoDestinoEnvio = ultimoDestinoEnvio;
        this.ultimoWhatsappShareUrl = ultimoWhatsappShareUrl;
        this.ultimoDetalheEnvio = ultimoDetalheEnvio;
        this.ultimoEnvioEm = ultimoEnvioEm;
        this.tentativasEnvio = tentativasEnvio;
    }

    public static Convite gerar(UUID imovelId, UUID unidadeId, UUID proprietarioId,
                                 CondicoesConvite condicoes, Instant agora) {
        Objects.requireNonNull(condicoes, "condições obrigatórias");
        return new Convite(UUID.randomUUID(), imovelId, unidadeId, proprietarioId,
            gerarToken(), agora.plus(7, ChronoUnit.DAYS), condicoes,
            ConviteStatus.PENDENTE, null,
            null, null, null, null, null, null, 0);
    }

    public static Convite reconstituir(UUID id, UUID imovelId, UUID unidadeId, UUID proprietarioId,
                                        String token, Instant expiraEm, CondicoesConvite condicoes,
                                        ConviteStatus status, UUID candidaturaId,
                                        CanalEnvioConvite ultimoCanalEnvio,
                                        String ultimoStatusEnvio,
                                        String ultimoDestinoEnvio,
                                        String ultimoWhatsappShareUrl,
                                        String ultimoDetalheEnvio,
                                        Instant ultimoEnvioEm,
                                        int tentativasEnvio) {
        return new Convite(id, imovelId, unidadeId, proprietarioId, token, expiraEm, condicoes,
            status, candidaturaId,
            ultimoCanalEnvio,
            ultimoStatusEnvio,
            ultimoDestinoEnvio,
            ultimoWhatsappShareUrl,
            ultimoDetalheEnvio,
            ultimoEnvioEm,
            tentativasEnvio);
    }

    public void registrarEnvio(CanalEnvioConvite canal,
                               String statusEnvio,
                               String destino,
                               String whatsappShareUrl,
                               String detalhe,
                               Instant enviadoEm) {
        this.ultimoCanalEnvio = canal;
        this.ultimoStatusEnvio = statusEnvio;
        this.ultimoDestinoEnvio = destino;
        this.ultimoWhatsappShareUrl = whatsappShareUrl;
        this.ultimoDetalheEnvio = detalhe;
        this.ultimoEnvioEm = enviadoEm;
        this.tentativasEnvio += 1;
    }

    public boolean expirado(Instant agora) {
        return agora.isAfter(expiraEm);
    }

    public void marcarCandidaturaCriada(UUID candidaturaId) {
        if (status != ConviteStatus.PENDENTE) {
            throw new IllegalStateException("convite não está pendente: " + status);
        }
        this.status = ConviteStatus.EM_ANALISE;
        this.candidaturaId = candidaturaId;
    }

    public void marcarConsumido() {
        this.status = ConviteStatus.CONSUMIDO;
    }

    private static String gerarToken() {
        byte[] bytes = new byte[24];
        RNG.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public UUID id() { return id; }
    public UUID imovelId() { return imovelId; }
    public UUID unidadeId() { return unidadeId; }
    public UUID proprietarioId() { return proprietarioId; }
    public String token() { return token; }
    public Instant expiraEm() { return expiraEm; }
    public CondicoesConvite condicoes() { return condicoes; }
    public ConviteStatus status() { return status; }
    public UUID candidaturaId() { return candidaturaId; }
    public CanalEnvioConvite ultimoCanalEnvio() { return ultimoCanalEnvio; }
    public String ultimoStatusEnvio() { return ultimoStatusEnvio; }
    public String ultimoDestinoEnvio() { return ultimoDestinoEnvio; }
    public String ultimoWhatsappShareUrl() { return ultimoWhatsappShareUrl; }
    public String ultimoDetalheEnvio() { return ultimoDetalheEnvio; }
    public Instant ultimoEnvioEm() { return ultimoEnvioEm; }
    public int tentativasEnvio() { return tentativasEnvio; }

    public record CondicoesConvite(
        TipoContrato tipoContrato,
        Dinheiro valorAluguel,
        Periodo periodoSugerido,
        GarantiaTipo garantiaAceita
    ) {}
}
