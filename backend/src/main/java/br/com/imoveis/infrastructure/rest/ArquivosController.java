package br.com.imoveis.infrastructure.rest;

import br.com.imoveis.application.exception.NaoEncontradoException;
import br.com.imoveis.application.ports.ArquivoRepository;
import br.com.imoveis.application.ports.ArquivoStorage;
import br.com.imoveis.application.ports.ContratoRepository;
import br.com.imoveis.application.ports.ConviteRepository;
import br.com.imoveis.domain.arquivo.Arquivo;
import br.com.imoveis.domain.arquivo.TipoArquivo;
import br.com.imoveis.domain.contrato.Contrato;
import br.com.imoveis.domain.convite.Candidatura;
import br.com.imoveis.infrastructure.auth.CurrentPrincipal;
import br.com.imoveis.infrastructure.auth.Principal;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.Duration;
import java.util.UUID;

@Controller("/arquivos")
@Tag(name = "Arquivos")
public class ArquivosController {

    private static final Duration VALIDADE_SAS = Duration.ofMinutes(10);

    private final ArquivoRepository arquivoRepository;
    private final ArquivoStorage arquivoStorage;
    private final ContratoRepository contratoRepository;
    private final ConviteRepository conviteRepository;

    public ArquivosController(ArquivoRepository arquivoRepository, ArquivoStorage arquivoStorage,
                               ContratoRepository contratoRepository, ConviteRepository conviteRepository) {
        this.arquivoRepository = arquivoRepository;
        this.arquivoStorage = arquivoStorage;
        this.contratoRepository = contratoRepository;
        this.conviteRepository = conviteRepository;
    }

    @ExecuteOn(TaskExecutors.BLOCKING)
    @Get(value = "/{id}/url", produces = MediaType.APPLICATION_JSON)
    public UrlResponse url(@PathVariable UUID id, HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        Arquivo arquivo = arquivoRepository.findById(id).orElseThrow(() -> new NaoEncontradoException("arquivo"));

        if (!arquivo.tipo().privado()) {
            return new UrlResponse(arquivoStorage.urlPublica(arquivo.tipo().container(), arquivo.blobKey()));
        }

        if (arquivo.tipo() == TipoArquivo.DOCUMENTO_CONTRATO || arquivo.tipo() == TipoArquivo.DOCUMENTO_GARANTIA) {
            // donoId de um documento de garantia pode ser um contratoId
            // (upload feito na revisão do contrato já aprovado) ou uma
            // candidaturaId (upload feito ainda na etapa de candidatura,
            // antes de existir contrato — ver AdicionarDocumentoGarantia).
            // Documento de contrato só existe no caminho contratoId.
            boolean autorizado = contratoRepository.findById(arquivo.donoId())
                .map(contrato -> isOwnerOrTenant(p, contrato))
                .orElseGet(() -> arquivo.tipo() == TipoArquivo.DOCUMENTO_GARANTIA && autorizadoViaCandidatura(p, arquivo.donoId()));
            if (!autorizado) {
                throw new NaoEncontradoException("arquivo");
            }
        }

        String url = arquivoStorage.urlTemporaria(arquivo.tipo().container(), arquivo.blobKey(), VALIDADE_SAS);
        return new UrlResponse(url);
    }

    private boolean autorizadoViaCandidatura(Principal p, UUID candidaturaId) {
        return conviteRepository.findCandidaturaById(candidaturaId)
            .flatMap(candidatura -> conviteRepository.findById(candidatura.conviteId())
                .map(convite -> isOwnerOrTenant(p, candidatura, convite.proprietarioId())))
            .orElse(false);
    }

    private boolean isOwnerOrTenant(Principal p, Candidatura c, UUID proprietarioId) {
        return (p.proprietarioId() != null && proprietarioId.equals(p.proprietarioId()))
            || (p.inquilinoId() != null && c.inquilinoId().equals(p.inquilinoId()));
    }

    private boolean isOwnerOrTenant(Principal p, Contrato c) {
        return (p.proprietarioId() != null && c.proprietarioId().equals(p.proprietarioId()))
            || (p.inquilinoId() != null && c.inquilinoId().equals(p.inquilinoId()));
    }

    @io.micronaut.serde.annotation.Serdeable
    public record UrlResponse(String url) {}
}
