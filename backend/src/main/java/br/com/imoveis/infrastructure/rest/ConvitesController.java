package br.com.imoveis.infrastructure.rest;

import br.com.imoveis.application.exception.NaoEncontradoException;
import br.com.imoveis.application.ports.ArquivoRepository;
import br.com.imoveis.application.ports.Clock;
import br.com.imoveis.application.ports.ConviteLinkSender;
import br.com.imoveis.application.ports.ContratoRepository;
import br.com.imoveis.application.ports.ConviteRepository;
import br.com.imoveis.application.ports.EventoAuditoriaRepository;
import br.com.imoveis.application.ports.ImovelRepository;
import br.com.imoveis.application.ports.ProprietarioRepository;
import br.com.imoveis.application.usecase.AceitarConviteComContaExistente;
import br.com.imoveis.application.usecase.AceitarConvite;
import br.com.imoveis.application.usecase.AdicionarDocumentoGarantia;
import br.com.imoveis.application.usecase.AssinarContratoPorConvite;
import br.com.imoveis.application.usecase.EnviarGarantiaDaCandidatura;
import br.com.imoveis.application.usecase.EnviarLinkConvite;
import br.com.imoveis.application.usecase.GerarConvite;
import br.com.imoveis.application.usecase.RegistrarEventoAuditoria;
import br.com.imoveis.application.usecase.RevogarConvite;
import br.com.imoveis.domain.arquivo.Arquivo;
import br.com.imoveis.domain.arquivo.TipoArquivo;
import br.com.imoveis.domain.auditoria.EntidadeAuditoria;
import br.com.imoveis.domain.auditoria.TipoEventoAuditoria;
import br.com.imoveis.domain.contrato.Contrato;
import br.com.imoveis.domain.convite.Candidatura;
import br.com.imoveis.domain.contrato.GarantiaTipo;
import br.com.imoveis.domain.contrato.TipoContrato;
import br.com.imoveis.domain.convite.CanalEnvioConvite;
import br.com.imoveis.domain.convite.Convite;
import br.com.imoveis.domain.convite.ConviteStatus;
import br.com.imoveis.domain.imovel.Imovel;
import br.com.imoveis.domain.shared.Dinheiro;
import br.com.imoveis.domain.shared.Periodo;
import br.com.imoveis.infrastructure.auth.CurrentPrincipal;
import br.com.imoveis.infrastructure.auth.Principal;
import br.com.imoveis.infrastructure.rest.dto.ContratoDtos.ArquivoInfoResponse;
import br.com.imoveis.infrastructure.rest.dto.ContratoDtos.ContratoResponse;
import br.com.imoveis.infrastructure.rest.dto.ConviteDtos.*;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.http.annotation.Status;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Controller
@Tag(name = "Convites")
public class ConvitesController {

    private final GerarConvite gerarConvite;
    private final AceitarConvite aceitarConvite;
    private final AceitarConviteComContaExistente aceitarConviteComContaExistente;
    private final AssinarContratoPorConvite assinarContratoPorConvite;
    private final EnviarGarantiaDaCandidatura enviarGarantia;
    private final EnviarLinkConvite enviarLinkConvite;
    private final RevogarConvite revogarConvite;
    private final AdicionarDocumentoGarantia adicionarDocumentoGarantia;
    private final ConviteRepository conviteRepository;
    private final ContratoRepository contratoRepository;
    private final ImovelRepository imovelRepository;
    private final ProprietarioRepository proprietarioRepository;
    private final ArquivoRepository arquivoRepository;
    private final RegistrarEventoAuditoria registrarEventoAuditoria;
    private final EventoAuditoriaRepository eventoAuditoriaRepository;
    private final Clock clock;

    public ConvitesController(GerarConvite gerarConvite, AceitarConvite aceitarConvite,
                               AceitarConviteComContaExistente aceitarConviteComContaExistente,
                               AssinarContratoPorConvite assinarContratoPorConvite,
                               EnviarGarantiaDaCandidatura enviarGarantia,
                               EnviarLinkConvite enviarLinkConvite,
                               RevogarConvite revogarConvite,
                               AdicionarDocumentoGarantia adicionarDocumentoGarantia,
                               ConviteRepository conviteRepository,
                               ContratoRepository contratoRepository,
                               ImovelRepository imovelRepository,
                               ProprietarioRepository proprietarioRepository,
                               ArquivoRepository arquivoRepository,
                               RegistrarEventoAuditoria registrarEventoAuditoria,
                               EventoAuditoriaRepository eventoAuditoriaRepository,
                               Clock clock) {
        this.gerarConvite = gerarConvite;
        this.aceitarConvite = aceitarConvite;
        this.aceitarConviteComContaExistente = aceitarConviteComContaExistente;
        this.assinarContratoPorConvite = assinarContratoPorConvite;
        this.enviarGarantia = enviarGarantia;
        this.enviarLinkConvite = enviarLinkConvite;
        this.revogarConvite = revogarConvite;
        this.adicionarDocumentoGarantia = adicionarDocumentoGarantia;
        this.conviteRepository = conviteRepository;
        this.contratoRepository = contratoRepository;
        this.imovelRepository = imovelRepository;
        this.proprietarioRepository = proprietarioRepository;
        this.arquivoRepository = arquivoRepository;
        this.registrarEventoAuditoria = registrarEventoAuditoria;
        this.eventoAuditoriaRepository = eventoAuditoriaRepository;
        this.clock = clock;
    }

    @Post(value = "/convites/{token}/revogar", produces = MediaType.APPLICATION_JSON)
    public ConviteResponse revogar(@PathVariable String token, HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        Convite convite = revogarConvite.execute(token, p.requireProprietarioId());
        return toResponse(convite);
    }

    @Get(value = "/convites", produces = MediaType.APPLICATION_JSON)
    public List<ConviteResponse> listarDoProprietario(HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        return conviteRepository.findByProprietarioId(p.requireProprietarioId()).stream()
            .map(this::toResponse)
            .toList();
    }

    @Get(value = "/imoveis/{imovelId}/convites", produces = MediaType.APPLICATION_JSON)
    public List<ConviteResponse> listarPorImovel(@PathVariable UUID imovelId, HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        Imovel imovel = imovelRepository.findById(imovelId)
            .filter(i -> i.proprietarioId().equals(p.proprietarioId()))
            .orElseThrow(() -> new NaoEncontradoException("imóvel"));
        return conviteRepository.findByImovelId(imovel.id()).stream()
            .filter(c -> c.ativo(clock.now()))
            .map(this::toResponse)
            .toList();
    }

    @Status(HttpStatus.CREATED)
    @Post(value = "/imoveis/{imovelId}/convites", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    public ConviteResponse gerar(@PathVariable UUID imovelId, @Body @Valid NovoConviteRequest body, HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        Convite.CondicoesConvite condicoes = new Convite.CondicoesConvite(
            body.tipoContrato() == null ? TipoContrato.RESIDENCIAL : body.tipoContrato(),
            new Dinheiro(body.valorAluguel()),
            new Periodo(body.dataInicio(), body.dataFim()),
            body.garantiaAceita());
        Convite convite = gerarConvite.execute(imovelId, body.unidadeId(), p.proprietarioId(), condicoes, body.emailInquilino());
        enviarLinkConvite.execute(
            convite,
            body.canalEnvio(),
            body.emailInquilino(),
            body.telefoneInquilino());
        return toResponse(convite);
    }

    @Get(value = "/convites/{token}", produces = MediaType.APPLICATION_JSON)
    public ConviteResponse buscarPorToken(@PathVariable String token) {
        return conviteRepository.findByToken(token).map(this::toResponse)
            .orElseThrow(() -> new NaoEncontradoException("convite"));
    }

    @Post(value = "/convites/{token}/reenviar", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    public ConviteResponse reenviar(@PathVariable String token,
                                    @Body @Valid ReenviarConviteRequest body,
                                    HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        UUID proprietarioId = p.requireProprietarioId();
        Convite convite = conviteRepository.findByToken(token)
            .orElseThrow(() -> new NaoEncontradoException("convite"));

        if (!convite.proprietarioId().equals(proprietarioId)) {
            throw new NaoEncontradoException("convite");
        }

        if (body.canalEnvio() == null) {
            throw new IllegalArgumentException("canal de envio é obrigatório para reenvio");
        }

        // Reenviar um convite expirado (mas ainda pendente, sem candidatura
        // criada) estende o prazo em vez de só reenviar um link que ia dar
        // "expirado" de novo assim que o inquilino tentasse aceitar — ver
        // Convite.renovar. Rank 5 do backlog técnico: recuperação de acesso.
        if (convite.status() == ConviteStatus.PENDENTE && convite.expirado(clock.now())) {
            convite.renovar(clock.now());
            conviteRepository.save(convite);
            registrarEventoAuditoria.execute(EntidadeAuditoria.CONVITE, convite.id(), TipoEventoAuditoria.RENOVADO, null);
        }

        // Se o corpo não repetir o destino, reaproveita o último destino
        // enviado nesse mesmo canal — evita o proprietário ter que digitar
        // de novo o e-mail/telefone só pra reenviar um link já existente.
        String emailInquilino = body.emailInquilino() != null ? body.emailInquilino()
            : (body.canalEnvio() == CanalEnvioConvite.EMAIL ? convite.ultimoDestinoEnvio() : null);
        String telefoneInquilino = body.telefoneInquilino() != null ? body.telefoneInquilino()
            : (body.canalEnvio() == CanalEnvioConvite.WHATSAPP ? convite.ultimoDestinoEnvio() : null);

        enviarLinkConvite.execute(convite, body.canalEnvio(), emailInquilino, telefoneInquilino);

        return toResponse(convite);
    }

    @Get(value = "/convites/{token}/eventos", produces = MediaType.APPLICATION_JSON)
    public List<EventoAuditoriaResponse> eventos(@PathVariable String token, HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        UUID proprietarioId = p.requireProprietarioId();
        Convite convite = conviteRepository.findByToken(token)
            .orElseThrow(() -> new NaoEncontradoException("convite"));
        if (!convite.proprietarioId().equals(proprietarioId)) {
            throw new NaoEncontradoException("convite");
        }
        return eventoAuditoriaRepository.findByEntidade(EntidadeAuditoria.CONVITE, convite.id()).stream()
            .map(EventoAuditoriaResponse::from)
            .toList();
    }

    private ConviteResponse toResponse(Convite c) {
        return ConviteResponse.from(c, c.expirado(clock.now()));
    }

    @Status(HttpStatus.CREATED)
    @Post(value = "/convites/{token}/cadastro", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    public CandidaturaResponse cadastro(@PathVariable String token, @Body @Valid CadastroInquilinoRequest body) {
        AceitarConvite.Result r = aceitarConvite.execute(token, body.username(), body.cpf(), body.email(), body.senha());
        return CandidaturaResponse.from(r.candidatura());
    }

    @Status(HttpStatus.CREATED)
    @Post(value = "/convites/{token}/aceitar-vinculo", produces = MediaType.APPLICATION_JSON)
    public CandidaturaResponse aceitarVinculo(@PathVariable String token, HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        var result = aceitarConviteComContaExistente.execute(token, p.requireInquilinoId());
        return CandidaturaResponse.from(result.candidatura());
    }

    @Post(value = "/convites/{token}/garantia", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    public CandidaturaResponse garantia(@PathVariable String token, @Body GarantiaRequest body) {
        return CandidaturaResponse.from(enviarGarantia.execute(token, body.tipo(), body.dadosEspecificos()));
    }

    // Upload do documento comprobatório da garantia (RG, comprovante de
    // renda, apólice) ainda na etapa de candidatura — antes de existir
    // contrato, então o donoId do ARQUIVO é a candidaturaId, não uma
    // contratoId (ver AdicionarDocumentoGarantia). Rota pública por token,
    // igual a /convites/{token}/garantia, já que o inquilino pode ainda não
    // ter sessão autenticada neste ponto do fluxo.
    @ExecuteOn(TaskExecutors.BLOCKING)
    @Status(HttpStatus.CREATED)
    @Post(value = "/convites/{token}/garantia/documentos", consumes = MediaType.MULTIPART_FORM_DATA, produces = MediaType.APPLICATION_JSON)
    public ArquivoInfoResponse adicionarDocumentoGarantiaPorConvite(@PathVariable String token, CompletedFileUpload documento) {
        UUID candidaturaId = candidaturaIdDoConvite(token);
        String contentType = documento.getContentType().map(MediaType::toString).orElse(null);
        try {
            Arquivo arquivo = adicionarDocumentoGarantia.execute(candidaturaId, documento.getFilename(), contentType,
                documento.getSize(), documento.getInputStream());
            return toArquivoInfo(arquivo);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Get(value = "/convites/{token}/garantia/documentos", produces = MediaType.APPLICATION_JSON)
    public List<ArquivoInfoResponse> documentosGarantiaPorConvite(@PathVariable String token) {
        UUID candidaturaId = candidaturaIdDoConvite(token);
        return arquivoRepository.findByDonoIdAndTipo(candidaturaId, TipoArquivo.DOCUMENTO_GARANTIA).stream()
            .map(this::toArquivoInfo)
            .toList();
    }

    private UUID candidaturaIdDoConvite(String token) {
        Convite convite = conviteRepository.findByToken(token)
            .orElseThrow(() -> new NaoEncontradoException("convite"));
        if (convite.candidaturaId() == null) {
            throw new IllegalStateException("candidatura ainda não iniciada para este convite");
        }
        return convite.candidaturaId();
    }

    private ArquivoInfoResponse toArquivoInfo(Arquivo arquivo) {
        return new ArquivoInfoResponse(arquivo.id(), arquivo.nomeOriginal());
    }

    @Get(value = "/convites/me", produces = MediaType.APPLICATION_JSON)
    public List<ConviteInquilinoResponse> convitesDoInquilino(HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        UUID inquilinoId = p.requireInquilinoId();

        return conviteRepository.findCandidaturasByInquilinoId(inquilinoId).stream()
            .map(candidatura -> toInquilinoResponse(inquilinoId, candidatura))
            .sorted(Comparator.comparing(ConviteInquilinoResponse::criadaEm).reversed())
            .toList();
    }

    @Post(value = "/convites/{token}/assinar", produces = MediaType.APPLICATION_JSON)
    public ContratoResponse assinarComoInquilino(@PathVariable String token) {
        Contrato contrato = assinarContratoPorConvite.execute(token);
        String nomeProprietario = proprietarioRepository.findById(contrato.proprietarioId())
            .map(br.com.imoveis.domain.proprietario.Proprietario::nome)
            .orElse(null);
        Imovel imovel = imovelRepository.findUnidadeById(contrato.unidadeId())
            .flatMap(unidade -> imovelRepository.findById(unidade.imovelId()))
            .orElse(null);
        String enderecoImovel = imovel == null ? null : imovel.enderecoCompleto();
        UUID imovelId = imovel == null ? null : imovel.id();
        return ContratoResponse.from(contrato, nomeProprietario, enderecoImovel, imovelId);
    }

    private ConviteInquilinoResponse toInquilinoResponse(UUID inquilinoId, Candidatura candidatura) {
        Convite convite = conviteRepository.findById(candidatura.conviteId())
            .orElseThrow(() -> new NaoEncontradoException("convite"));

        var contrato = contratoRepository.findByUnidadeInquilinoEPeriodo(
            convite.unidadeId(),
            inquilinoId,
            convite.condicoes().periodoSugerido().inicio(),
            convite.condicoes().periodoSugerido().fim());

        String enderecoImovel = imovelRepository.findById(convite.imovelId())
            .map(Imovel::enderecoCompleto)
            .orElse(null);
        String nomeProprietario = proprietarioRepository.findById(convite.proprietarioId())
            .map(br.com.imoveis.domain.proprietario.Proprietario::nome)
            .orElse(null);

        return new ConviteInquilinoResponse(
            convite.id(),
            convite.token(),
            convite.imovelId(),
            enderecoImovel,
            convite.unidadeId(),
            convite.proprietarioId(),
            nomeProprietario,
            convite.status(),
            candidatura.id(),
            candidatura.status(),
            convite.condicoes().tipoContrato(),
            convite.condicoes().valorAluguel().valor(),
            convite.condicoes().periodoSugerido().inicio(),
            convite.condicoes().periodoSugerido().fim(),
            convite.condicoes().garantiaAceita(),
            candidatura.garantiaEscolhida(),
            candidatura.criadaEm(),
            contrato.map(br.com.imoveis.domain.contrato.Contrato::id).orElse(null),
            contrato.map(br.com.imoveis.domain.contrato.Contrato::statusAssinatura).orElse(null));
    }
}
