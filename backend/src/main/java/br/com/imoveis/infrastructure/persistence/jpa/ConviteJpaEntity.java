package br.com.imoveis.infrastructure.persistence.jpa;

import br.com.imoveis.domain.contrato.GarantiaTipo;
import br.com.imoveis.domain.contrato.TipoContrato;
import br.com.imoveis.domain.convite.CanalEnvioConvite;
import br.com.imoveis.domain.convite.ConviteStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "convites")
public class ConviteJpaEntity {

    @Id
    private UUID id;

    @Column(name = "imovel_id", nullable = false)
    private UUID imovelId;

    @Column(name = "unidade_id", nullable = false)
    private UUID unidadeId;

    @Column(name = "proprietario_id", nullable = false)
    private UUID proprietarioId;

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_contrato", nullable = false)
    private TipoContrato tipoContrato;

    @Column(name = "valor_aluguel", nullable = false)
    private BigDecimal valorAluguel;

    @Column(name = "periodo_inicio", nullable = false)
    private LocalDate periodoInicio;

    @Column(name = "periodo_fim", nullable = false)
    private LocalDate periodoFim;

    @Enumerated(EnumType.STRING)
    @Column(name = "garantia_aceita", nullable = false)
    private GarantiaTipo garantiaAceita;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConviteStatus status;

    @Column(name = "candidatura_id")
    private UUID candidaturaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ultimo_canal_envio")
    private CanalEnvioConvite ultimoCanalEnvio;

    @Column(name = "ultimo_status_envio")
    private String ultimoStatusEnvio;

    @Column(name = "ultimo_destino_envio")
    private String ultimoDestinoEnvio;

    @Column(name = "ultimo_whatsapp_share_url", columnDefinition = "TEXT")
    private String ultimoWhatsappShareUrl;

    @Column(name = "ultimo_detalhe_envio", columnDefinition = "TEXT")
    private String ultimoDetalheEnvio;

    @Column(name = "ultimo_envio_em")
    private Instant ultimoEnvioEm;

    @Column(name = "tentativas_envio", nullable = false)
    private int tentativasEnvio;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getImovelId() { return imovelId; }
    public void setImovelId(UUID imovelId) { this.imovelId = imovelId; }

    public UUID getUnidadeId() { return unidadeId; }
    public void setUnidadeId(UUID unidadeId) { this.unidadeId = unidadeId; }

    public UUID getProprietarioId() { return proprietarioId; }
    public void setProprietarioId(UUID proprietarioId) { this.proprietarioId = proprietarioId; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Instant getExpiraEm() { return expiraEm; }
    public void setExpiraEm(Instant expiraEm) { this.expiraEm = expiraEm; }

    public TipoContrato getTipoContrato() { return tipoContrato; }
    public void setTipoContrato(TipoContrato tipoContrato) { this.tipoContrato = tipoContrato; }

    public BigDecimal getValorAluguel() { return valorAluguel; }
    public void setValorAluguel(BigDecimal valorAluguel) { this.valorAluguel = valorAluguel; }

    public LocalDate getPeriodoInicio() { return periodoInicio; }
    public void setPeriodoInicio(LocalDate periodoInicio) { this.periodoInicio = periodoInicio; }

    public LocalDate getPeriodoFim() { return periodoFim; }
    public void setPeriodoFim(LocalDate periodoFim) { this.periodoFim = periodoFim; }

    public GarantiaTipo getGarantiaAceita() { return garantiaAceita; }
    public void setGarantiaAceita(GarantiaTipo garantiaAceita) { this.garantiaAceita = garantiaAceita; }

    public ConviteStatus getStatus() { return status; }
    public void setStatus(ConviteStatus status) { this.status = status; }

    public UUID getCandidaturaId() { return candidaturaId; }
    public void setCandidaturaId(UUID candidaturaId) { this.candidaturaId = candidaturaId; }

    public CanalEnvioConvite getUltimoCanalEnvio() { return ultimoCanalEnvio; }
    public void setUltimoCanalEnvio(CanalEnvioConvite ultimoCanalEnvio) { this.ultimoCanalEnvio = ultimoCanalEnvio; }

    public String getUltimoStatusEnvio() { return ultimoStatusEnvio; }
    public void setUltimoStatusEnvio(String ultimoStatusEnvio) { this.ultimoStatusEnvio = ultimoStatusEnvio; }

    public String getUltimoDestinoEnvio() { return ultimoDestinoEnvio; }
    public void setUltimoDestinoEnvio(String ultimoDestinoEnvio) { this.ultimoDestinoEnvio = ultimoDestinoEnvio; }

    public String getUltimoWhatsappShareUrl() { return ultimoWhatsappShareUrl; }
    public void setUltimoWhatsappShareUrl(String ultimoWhatsappShareUrl) { this.ultimoWhatsappShareUrl = ultimoWhatsappShareUrl; }

    public String getUltimoDetalheEnvio() { return ultimoDetalheEnvio; }
    public void setUltimoDetalheEnvio(String ultimoDetalheEnvio) { this.ultimoDetalheEnvio = ultimoDetalheEnvio; }

    public Instant getUltimoEnvioEm() { return ultimoEnvioEm; }
    public void setUltimoEnvioEm(Instant ultimoEnvioEm) { this.ultimoEnvioEm = ultimoEnvioEm; }

    public int getTentativasEnvio() { return tentativasEnvio; }
    public void setTentativasEnvio(int tentativasEnvio) { this.tentativasEnvio = tentativasEnvio; }
}
