package br.com.imoveis.infrastructure.storage;

import br.com.imoveis.application.ports.ArquivoStorage;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;

import java.io.InputStream;
import java.time.Duration;
import java.time.OffsetDateTime;

@Singleton
public class AzureBlobStorageAdapter implements ArquivoStorage {

    private final BlobServiceClient client;

    public AzureBlobStorageAdapter(@Value("${app.storage.connection-string:}") String connectionString) {
        this.client = connectionString == null || connectionString.isBlank()
            ? null
            : new BlobServiceClientBuilder().connectionString(connectionString).buildClient();
    }

    @Override
    public void upload(String container, String blobKey, InputStream dados, long tamanho, String contentType) {
        BlobClient blobClient = blobClient(container, blobKey);
        blobClient.upload(dados, tamanho, true);
        blobClient.setHttpHeaders(new BlobHttpHeaders().setContentType(contentType));
    }

    @Override
    public String urlPublica(String container, String blobKey) {
        return blobClient(container, blobKey).getBlobUrl();
    }

    @Override
    public String urlTemporaria(String container, String blobKey, Duration validade) {
        BlobClient blobClient = blobClient(container, blobKey);
        BlobSasPermission permissao = new BlobSasPermission().setReadPermission(true);
        OffsetDateTime expiraEm = OffsetDateTime.now().plus(validade);
        String sas = blobClient.generateSas(new BlobServiceSasSignatureValues(expiraEm, permissao));
        return blobClient.getBlobUrl() + "?" + sas;
    }

    @Override
    public void excluir(String container, String blobKey) {
        blobClient(container, blobKey).deleteIfExists();
    }

    private BlobClient blobClient(String container, String blobKey) {
        if (client == null) {
            throw new IllegalStateException("Azure Storage não configurado (AZURE_STORAGE_CONNECTION_STRING ausente)");
        }
        BlobContainerClient containerClient = client.getBlobContainerClient(container);
        return containerClient.getBlobClient(blobKey);
    }
}
