package br.com.imoveis.domain.arquivo;

import java.time.Instant;
import java.util.UUID;

public class Arquivo {

    private final UUID id;
    private final TipoArquivo tipo;
    private final UUID donoId;
    private final String blobKey;
    private final String nomeOriginal;
    private final String contentType;
    private final long tamanhoBytes;
    private final Instant criadoEm;

    private Arquivo(UUID id, TipoArquivo tipo, UUID donoId, String blobKey, String nomeOriginal,
                    String contentType, long tamanhoBytes, Instant criadoEm) {
        this.id = id;
        this.tipo = tipo;
        this.donoId = donoId;
        this.blobKey = blobKey;
        this.nomeOriginal = nomeOriginal;
        this.contentType = contentType;
        this.tamanhoBytes = tamanhoBytes;
        this.criadoEm = criadoEm;
    }

    public static Arquivo novo(TipoArquivo tipo, UUID donoId, String blobKey, String nomeOriginal,
                                String contentType, long tamanhoBytes, Instant agora) {
        return new Arquivo(UUID.randomUUID(), tipo, donoId, blobKey, nomeOriginal, contentType, tamanhoBytes, agora);
    }

    public static Arquivo reconstituir(UUID id, TipoArquivo tipo, UUID donoId, String blobKey, String nomeOriginal,
                                        String contentType, long tamanhoBytes, Instant criadoEm) {
        return new Arquivo(id, tipo, donoId, blobKey, nomeOriginal, contentType, tamanhoBytes, criadoEm);
    }

    public UUID id() { return id; }
    public TipoArquivo tipo() { return tipo; }
    public UUID donoId() { return donoId; }
    public String blobKey() { return blobKey; }
    public String nomeOriginal() { return nomeOriginal; }
    public String contentType() { return contentType; }
    public long tamanhoBytes() { return tamanhoBytes; }
    public Instant criadoEm() { return criadoEm; }
}
