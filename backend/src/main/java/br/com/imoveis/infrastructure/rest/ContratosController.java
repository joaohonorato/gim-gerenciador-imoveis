package br.com.imoveis.infrastructure.rest;

import br.com.imoveis.application.exception.NaoEncontradoException;
import br.com.imoveis.application.ports.ArquivoRepository;
import br.com.imoveis.application.ports.ContratoRepository;
import br.com.imoveis.application.usecase.AdicionarDocumentoContrato;
import br.com.imoveis.application.usecase.AdicionarDocumentoGarantia;
import br.com.imoveis.application.usecase.AssinarContrato;
import br.com.imoveis.domain.arquivo.Arquivo;
import br.com.imoveis.domain.arquivo.TipoArquivo;
import br.com.imoveis.domain.contrato.Contrato;
import br.com.imoveis.infrastructure.auth.CurrentPrincipal;
import br.com.imoveis.infrastructure.auth.Principal;
import br.com.imoveis.infrastructure.rest.dto.ContratoDtos.*;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;

@Controller("/contratos")
public class ContratosController {

    private final AssinarContrato assinar;
    private final ContratoRepository contratoRepository;
    private final AdicionarDocumentoContrato adicionarDocumentoContrato;
    private final AdicionarDocumentoGarantia adicionarDocumentoGarantia;
    private final ArquivoRepository arquivoRepository;

    public ContratosController(AssinarContrato assinar, ContratoRepository contratoRepository,
                                AdicionarDocumentoContrato adicionarDocumentoContrato,
                                AdicionarDocumentoGarantia adicionarDocumentoGarantia,
                                ArquivoRepository arquivoRepository) {
        this.assinar = assinar;
        this.contratoRepository = contratoRepository;
        this.adicionarDocumentoContrato = adicionarDocumentoContrato;
        this.adicionarDocumentoGarantia = adicionarDocumentoGarantia;
        this.arquivoRepository = arquivoRepository;
    }

    @Get(produces = MediaType.APPLICATION_JSON)
    public List<ContratoResponse> listar(HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        List<Contrato> contratos = p.inquilinoId() != null
            ? contratoRepository.findByInquilinoId(p.inquilinoId())
            : contratoRepository.findByProprietarioId(p.requireProprietarioId());
        return contratos.stream().map(ContratoResponse::from).toList();
    }

    @Get(value = "/{id}", produces = MediaType.APPLICATION_JSON)
    public ContratoResponse detalhe(@PathVariable UUID id, HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        Contrato c = contratoRepository.findById(id)
            .filter(x -> isOwnerOrTenant(p, x))
            .orElseThrow(() -> new NaoEncontradoException("contrato"));
        return ContratoResponse.from(c);
    }

    @Post(value = "/{id}/assinar", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    public ContratoResponse assinar(@PathVariable UUID id, @Body AssinarRequest body, HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        Contrato contrato = contratoRepository.findById(id)
            .filter(x -> isOwnerOrTenant(p, x))
            .orElseThrow(() -> new NaoEncontradoException("contrato"));

        if (body.parte() == br.com.imoveis.domain.contrato.ParteContrato.PROPRIETARIO) {
            if (p.proprietarioId() == null || !contrato.proprietarioId().equals(p.proprietarioId())) {
                throw new NaoEncontradoException("contrato");
            }
        }

        if (body.parte() == br.com.imoveis.domain.contrato.ParteContrato.INQUILINO) {
            if (p.inquilinoId() == null || !contrato.inquilinoId().equals(p.inquilinoId())) {
                throw new NaoEncontradoException("contrato");
            }
        }

        return ContratoResponse.from(assinar.execute(id, body.parte()));
    }

    @Get(value = "/{id}/pagamentos", produces = MediaType.APPLICATION_JSON)
    public List<PagamentoResponse> pagamentos(@PathVariable UUID id, HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        contratoRepository.findById(id)
            .filter(x -> isOwnerOrTenant(p, x))
            .orElseThrow(() -> new NaoEncontradoException("contrato"));
        return contratoRepository.findPagamentosByContrato(id).stream().map(PagamentoResponse::from).toList();
    }

    @Get(value = "/{id}/documentos", produces = MediaType.APPLICATION_JSON)
    public DocumentosContratoResponse documentos(@PathVariable UUID id, HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        contratoRepository.findById(id)
            .filter(x -> isOwnerOrTenant(p, x))
            .orElseThrow(() -> new NaoEncontradoException("contrato"));

        ArquivoInfoResponse documentoContrato = arquivoRepository.findByDonoIdAndTipo(id, TipoArquivo.DOCUMENTO_CONTRATO)
            .stream().findFirst().map(this::toArquivoInfo).orElse(null);
        List<ArquivoInfoResponse> documentosGarantia = arquivoRepository.findByDonoIdAndTipo(id, TipoArquivo.DOCUMENTO_GARANTIA)
            .stream().map(this::toArquivoInfo).toList();
        return new DocumentosContratoResponse(documentoContrato, documentosGarantia);
    }

    @ExecuteOn(TaskExecutors.BLOCKING)
    @Status(HttpStatus.CREATED)
    @Post(value = "/{id}/documento", consumes = MediaType.MULTIPART_FORM_DATA, produces = MediaType.APPLICATION_JSON)
    public ArquivoInfoResponse adicionarDocumento(@PathVariable UUID id, CompletedFileUpload documento, HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        Contrato contrato = contratoRepository.findById(id)
            .filter(x -> isOwnerOrTenant(p, x))
            .orElseThrow(() -> new NaoEncontradoException("contrato"));
        String contentType = documento.getContentType().map(MediaType::toString).orElse(null);
        try {
            Arquivo arquivo = adicionarDocumentoContrato.execute(contrato.id(), documento.getFilename(), contentType,
                documento.getSize(), documento.getInputStream());
            return toArquivoInfo(arquivo);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @ExecuteOn(TaskExecutors.BLOCKING)
    @Status(HttpStatus.CREATED)
    @Post(value = "/{id}/garantia/documentos", consumes = MediaType.MULTIPART_FORM_DATA, produces = MediaType.APPLICATION_JSON)
    public ArquivoInfoResponse adicionarDocumentoGarantia(@PathVariable UUID id, CompletedFileUpload documento, HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        Contrato contrato = contratoRepository.findById(id)
            .filter(x -> isOwnerOrTenant(p, x))
            .orElseThrow(() -> new NaoEncontradoException("contrato"));
        String contentType = documento.getContentType().map(MediaType::toString).orElse(null);
        try {
            Arquivo arquivo = adicionarDocumentoGarantia.execute(contrato.id(), documento.getFilename(), contentType,
                documento.getSize(), documento.getInputStream());
            return toArquivoInfo(arquivo);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private ArquivoInfoResponse toArquivoInfo(Arquivo arquivo) {
        return new ArquivoInfoResponse(arquivo.id(), arquivo.nomeOriginal());
    }

    private boolean isOwnerOrTenant(Principal p, Contrato c) {
        return (p.proprietarioId() != null && c.proprietarioId().equals(p.proprietarioId()))
            || (p.inquilinoId() != null && c.inquilinoId().equals(p.inquilinoId()));
    }
}
