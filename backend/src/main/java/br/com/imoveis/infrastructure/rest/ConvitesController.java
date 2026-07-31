package br.com.imoveis.infrastructure.rest;

import br.com.imoveis.application.exception.NaoEncontradoException;
import br.com.imoveis.application.ports.ConviteLinkSender;
import br.com.imoveis.application.ports.ContratoRepository;
import br.com.imoveis.application.ports.ConviteRepository;
import br.com.imoveis.application.ports.ImovelRepository;
import br.com.imoveis.application.usecase.AceitarConviteComContaExistente;
import br.com.imoveis.application.usecase.AceitarConvite;
import br.com.imoveis.application.usecase.AssinarContratoPorConvite;
import br.com.imoveis.application.usecase.EnviarGarantiaDaCandidatura;
import br.com.imoveis.application.usecase.EnviarLinkConvite;
import br.com.imoveis.application.usecase.GerarConvite;
import br.com.imoveis.domain.contrato.Contrato;
import br.com.imoveis.domain.convite.Candidatura;
import br.com.imoveis.domain.contrato.GarantiaTipo;
import br.com.imoveis.domain.contrato.TipoContrato;
import br.com.imoveis.domain.convite.Convite;
import br.com.imoveis.domain.imovel.Imovel;
import br.com.imoveis.domain.shared.Dinheiro;
import br.com.imoveis.domain.shared.Periodo;
import br.com.imoveis.infrastructure.auth.CurrentPrincipal;
import br.com.imoveis.infrastructure.auth.Principal;
import br.com.imoveis.infrastructure.rest.dto.ContratoDtos.ContratoResponse;
import br.com.imoveis.infrastructure.rest.dto.ConviteDtos.*;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.http.annotation.Status;
import jakarta.validation.Valid;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Controller
public class ConvitesController {

    private final GerarConvite gerarConvite;
    private final AceitarConvite aceitarConvite;
    private final AceitarConviteComContaExistente aceitarConviteComContaExistente;
    private final AssinarContratoPorConvite assinarContratoPorConvite;
    private final EnviarGarantiaDaCandidatura enviarGarantia;
    private final EnviarLinkConvite enviarLinkConvite;
    private final ConviteRepository conviteRepository;
    private final ContratoRepository contratoRepository;
    private final ImovelRepository imovelRepository;

    public ConvitesController(GerarConvite gerarConvite, AceitarConvite aceitarConvite,
                               AceitarConviteComContaExistente aceitarConviteComContaExistente,
                               AssinarContratoPorConvite assinarContratoPorConvite,
                               EnviarGarantiaDaCandidatura enviarGarantia,
                               EnviarLinkConvite enviarLinkConvite,
                               ConviteRepository conviteRepository,
                               ContratoRepository contratoRepository,
                               ImovelRepository imovelRepository) {
        this.gerarConvite = gerarConvite;
        this.aceitarConvite = aceitarConvite;
        this.aceitarConviteComContaExistente = aceitarConviteComContaExistente;
        this.assinarContratoPorConvite = assinarContratoPorConvite;
        this.enviarGarantia = enviarGarantia;
        this.enviarLinkConvite = enviarLinkConvite;
        this.conviteRepository = conviteRepository;
        this.contratoRepository = contratoRepository;
        this.imovelRepository = imovelRepository;
    }

    @Get(value = "/imoveis/{imovelId}/convites", produces = MediaType.APPLICATION_JSON)
    public List<ConviteResponse> listarPorImovel(@PathVariable UUID imovelId, HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        Imovel imovel = imovelRepository.findById(imovelId)
            .filter(i -> i.proprietarioId().equals(p.proprietarioId()))
            .orElseThrow(() -> new NaoEncontradoException("imóvel"));
        return conviteRepository.findByImovelId(imovel.id()).stream()
            .map(ConviteResponse::from)
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
        Convite convite = gerarConvite.execute(imovelId, p.proprietarioId(), condicoes, body.emailInquilino());
        enviarLinkConvite.execute(
            convite,
            body.canalEnvio(),
            body.emailInquilino(),
            body.telefoneInquilino());
        return ConviteResponse.from(convite);
    }

    @Get(value = "/convites/{token}", produces = MediaType.APPLICATION_JSON)
    public ConviteResponse buscarPorToken(@PathVariable String token) {
        return conviteRepository.findByToken(token).map(ConviteResponse::from)
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

        enviarLinkConvite.execute(
            convite,
            body.canalEnvio(),
            body.emailInquilino(),
            body.telefoneInquilino());

        return ConviteResponse.from(convite);
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
        return ContratoResponse.from(contrato);
    }

    private ConviteInquilinoResponse toInquilinoResponse(UUID inquilinoId, Candidatura candidatura) {
        Convite convite = conviteRepository.findById(candidatura.conviteId())
            .orElseThrow(() -> new NaoEncontradoException("convite"));

        var contrato = contratoRepository.findByUnidadeInquilinoEPeriodo(
            convite.unidadeId(),
            inquilinoId,
            convite.condicoes().periodoSugerido().inicio(),
            convite.condicoes().periodoSugerido().fim());

        return new ConviteInquilinoResponse(
            convite.id(),
            convite.token(),
            convite.imovelId(),
            convite.unidadeId(),
            convite.proprietarioId(),
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
