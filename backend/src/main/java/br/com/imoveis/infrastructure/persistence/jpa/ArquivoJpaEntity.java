package br.com.imoveis.infrastructure.persistence.jpa;

import br.com.imoveis.domain.arquivo.TipoArquivo;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "arquivos")
public class ArquivoJpaEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoArquivo tipo;

    @Column(name = "dono_id", nullable = false)
    private UUID donoId;

    @Column(name = "blob_key", nullable = false, length = 500)
    private String blobKey;

    @Column(name = "nome_original")
    private String nomeOriginal;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "tamanho_bytes", nullable = false)
    private long tamanhoBytes;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public TipoArquivo getTipo() { return tipo; }
    public void setTipo(TipoArquivo tipo) { this.tipo = tipo; }

    public UUID getDonoId() { return donoId; }
    public void setDonoId(UUID donoId) { this.donoId = donoId; }

    public String getBlobKey() { return blobKey; }
    public void setBlobKey(String blobKey) { this.blobKey = blobKey; }

    public String getNomeOriginal() { return nomeOriginal; }
    public void setNomeOriginal(String nomeOriginal) { this.nomeOriginal = nomeOriginal; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public long getTamanhoBytes() { return tamanhoBytes; }
    public void setTamanhoBytes(long tamanhoBytes) { this.tamanhoBytes = tamanhoBytes; }

    public Instant getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Instant criadoEm) { this.criadoEm = criadoEm; }
}
