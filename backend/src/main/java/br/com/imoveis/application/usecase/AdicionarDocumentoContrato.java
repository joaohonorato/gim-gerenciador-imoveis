package br.com.imoveis.application.usecase;

import br.com.imoveis.application.ports.ArquivoRepository;
import br.com.imoveis.application.ports.ArquivoStorage;
import br.com.imoveis.application.ports.Clock;
import br.com.imoveis.domain.arquivo.Arquivo;
import br.com.imoveis.domain.arquivo.TipoArquivo;
import jakarta.inject.Singleton;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Singleton
public class AdicionarDocumentoContrato {

    private static final long TAMANHO_MAXIMO_BYTES = 10L * 1024 * 1024;
    private static final List<String> CONTENT_TYPES_PERMITIDOS = List.of(
        "application/pdf", "image/jpeg", "image/png", "image/webp");

    private final ArquivoRepository arquivoRepository;
    private final ArquivoStorage storage;
    private final Clock clock;

    public AdicionarDocumentoContrato(ArquivoRepository arquivoRepository, ArquivoStorage storage, Clock clock) {
        this.arquivoRepository = arquivoRepository;
        this.storage = storage;
        this.clock = clock;
    }

    public Arquivo execute(UUID contratoId, String nomeOriginal, String contentType, long tamanho, InputStream dados) {
        validar(contentType, tamanho);

        // documento do contrato é 1:1 — substitui o anterior, se existir
        arquivoRepository.findByDonoIdAndTipo(contratoId, TipoArquivo.DOCUMENTO_CONTRATO).forEach(antigo -> {
            storage.excluir(antigo.tipo().container(), antigo.blobKey());
            arquivoRepository.excluir(antigo.id());
        });

        String blobKey = contratoId + "/contrato/" + UUID.randomUUID() + extensao(contentType);
        storage.upload(TipoArquivo.DOCUMENTO_CONTRATO.container(), blobKey, dados, tamanho, contentType);

        Arquivo arquivo = Arquivo.novo(TipoArquivo.DOCUMENTO_CONTRATO, contratoId, blobKey, nomeOriginal, contentType, tamanho, clock.now());
        return arquivoRepository.save(arquivo);
    }

    static void validar(String contentType, long tamanho) {
        if (contentType == null || !CONTENT_TYPES_PERMITIDOS.contains(contentType)) {
            throw new IllegalArgumentException("tipo de arquivo não permitido: " + contentType);
        }
        if (tamanho > TAMANHO_MAXIMO_BYTES) {
            throw new IllegalArgumentException("arquivo excede o tamanho máximo permitido (10MB)");
        }
    }

    static String extensao(String contentType) {
        return switch (contentType) {
            case "application/pdf" -> ".pdf";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }
}
