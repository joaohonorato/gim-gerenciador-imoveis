package br.com.imoveis.application.usecase;

import br.com.imoveis.application.ports.ArquivoRepository;
import br.com.imoveis.application.ports.ArquivoStorage;
import br.com.imoveis.application.ports.Clock;
import br.com.imoveis.domain.arquivo.Arquivo;
import br.com.imoveis.domain.arquivo.TipoArquivo;
import jakarta.inject.Singleton;

import java.io.InputStream;
import java.util.UUID;

@Singleton
public class AdicionarDocumentoGarantia {

    private final ArquivoRepository arquivoRepository;
    private final ArquivoStorage storage;
    private final Clock clock;

    public AdicionarDocumentoGarantia(ArquivoRepository arquivoRepository, ArquivoStorage storage, Clock clock) {
        this.arquivoRepository = arquivoRepository;
        this.storage = storage;
        this.clock = clock;
    }

    public Arquivo execute(UUID contratoId, String nomeOriginal, String contentType, long tamanho, InputStream dados) {
        AdicionarDocumentoContrato.validar(contentType, tamanho);

        String blobKey = contratoId + "/garantia/" + UUID.randomUUID() + AdicionarDocumentoContrato.extensao(contentType);
        storage.upload(TipoArquivo.DOCUMENTO_GARANTIA.container(), blobKey, dados, tamanho, contentType);

        Arquivo arquivo = Arquivo.novo(TipoArquivo.DOCUMENTO_GARANTIA, contratoId, blobKey, nomeOriginal, contentType, tamanho, clock.now());
        return arquivoRepository.save(arquivo);
    }
}
