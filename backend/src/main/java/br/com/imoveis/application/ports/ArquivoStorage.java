package br.com.imoveis.application.ports;

import java.io.InputStream;
import java.time.Duration;

public interface ArquivoStorage {
    void upload(String container, String blobKey, InputStream dados, long tamanho, String contentType);
    String urlPublica(String container, String blobKey);
    String urlTemporaria(String container, String blobKey, Duration validade);
    void excluir(String container, String blobKey);
}
