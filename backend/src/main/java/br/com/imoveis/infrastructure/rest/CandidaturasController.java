package br.com.imoveis.infrastructure.rest;

import br.com.imoveis.application.exception.NaoEncontradoException;
import br.com.imoveis.application.ports.ConviteRepository;
import br.com.imoveis.application.ports.InquilinoRepository;
import br.com.imoveis.application.usecase.AprovarCandidato;
import br.com.imoveis.application.usecase.ListarCandidaturasPendentes;
import br.com.imoveis.application.usecase.RecusarCandidato;
import br.com.imoveis.domain.convite.Candidatura;
import br.com.imoveis.domain.convite.Convite;
import br.com.imoveis.domain.inquilino.Inquilino;
import br.com.imoveis.infrastructure.auth.CurrentPrincipal;
import br.com.imoveis.infrastructure.auth.Principal;
import br.com.imoveis.infrastructure.rest.dto.ContratoDtos.ContratoResponse;
import br.com.imoveis.infrastructure.rest.dto.ConviteDtos.CandidaturaPendenteResponse;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Status;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

@Controller("/candidaturas")
@Tag(name = "Candidaturas")
public class CandidaturasController {

    private final AprovarCandidato aprovar;
    private final RecusarCandidato recusar;
    private final ListarCandidaturasPendentes listarPendentes;
    private final ConviteRepository conviteRepository;
    private final InquilinoRepository inquilinoRepository;

    public CandidaturasController(AprovarCandidato aprovar, RecusarCandidato recusar,
                                   ListarCandidaturasPendentes listarPendentes,
                                   ConviteRepository conviteRepository,
                                   InquilinoRepository inquilinoRepository) {
        this.aprovar = aprovar;
        this.recusar = recusar;
        this.listarPendentes = listarPendentes;
        this.conviteRepository = conviteRepository;
        this.inquilinoRepository = inquilinoRepository;
    }

    @Get(produces = MediaType.APPLICATION_JSON)
    public List<CandidaturaPendenteResponse> listarPendentes(HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        UUID proprietarioId = p.requireProprietarioId();
        return listarPendentes.execute(proprietarioId).stream().map(this::toResponse).toList();
    }

    @Status(HttpStatus.CREATED)
    @Post(value = "/{id}/aprovar", produces = MediaType.APPLICATION_JSON)
    public ContratoResponse aprovar(@PathVariable UUID id, HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        return ContratoResponse.from(aprovar.execute(id, p.proprietarioId()));
    }

    @Post(value = "/{id}/recusar", produces = MediaType.APPLICATION_JSON)
    public void recusar(@PathVariable UUID id, HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        recusar.execute(id, p.proprietarioId());
    }

    private CandidaturaPendenteResponse toResponse(Candidatura c) {
        Convite convite = conviteRepository.findById(c.conviteId())
            .orElseThrow(() -> new NaoEncontradoException("convite"));
        Inquilino inquilino = inquilinoRepository.findById(c.inquilinoId()).orElse(null);
        return new CandidaturaPendenteResponse(
            c.id(),
            convite.imovelId(),
            convite.unidadeId(),
            c.inquilinoId(),
            inquilino == null ? null : inquilino.nome(),
            inquilino == null ? null : inquilino.email().value(),
            convite.condicoes().tipoContrato(),
            convite.condicoes().valorAluguel().valor(),
            convite.condicoes().periodoSugerido().inicio(),
            convite.condicoes().periodoSugerido().fim(),
            convite.condicoes().garantiaAceita(),
            c.garantiaEscolhida(),
            c.criadaEm());
    }
}
