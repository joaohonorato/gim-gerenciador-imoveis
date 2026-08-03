package br.com.imoveis.infrastructure.rest;

import br.com.imoveis.application.exception.NaoEncontradoException;
import br.com.imoveis.application.ports.ConviteAcessoRepository;
import br.com.imoveis.application.ports.MagicLinkRepository;
import br.com.imoveis.application.ports.TokenContaRepository;
import br.com.imoveis.domain.auth.TokenContaFinalidade;
import br.com.imoveis.domain.shared.Email;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.tags.Tag;

@Controller("/test-support")
@Requires(property = "app.test-support.enabled", value = "true")
@Tag(name = "Test Support")
public class TestSupportController {

    private final MagicLinkRepository magicLinkRepository;
    private final ConviteAcessoRepository conviteAcessoRepository;
    private final TokenContaRepository tokenContaRepository;

    public TestSupportController(MagicLinkRepository magicLinkRepository,
                                 ConviteAcessoRepository conviteAcessoRepository,
                                 TokenContaRepository tokenContaRepository) {
        this.magicLinkRepository = magicLinkRepository;
        this.conviteAcessoRepository = conviteAcessoRepository;
        this.tokenContaRepository = tokenContaRepository;
    }

    @Get(value = "/magic-links/{email}", produces = MediaType.APPLICATION_JSON)
    public MagicLinkTokenResponse latestForEmail(@PathVariable String email) {
        MagicLinkRepository.MagicLink link = magicLinkRepository.findMostRecentByEmail(new Email(email))
            .orElseThrow(() -> new NaoEncontradoException("magic-link"));
        return new MagicLinkTokenResponse(link.token());
    }

    @Get(value = "/access-invites/{email}", produces = MediaType.APPLICATION_JSON)
    public MagicLinkTokenResponse latestAccessInviteForEmail(@PathVariable String email) {
        var convite = conviteAcessoRepository.findMostRecentByEmail(new Email(email))
            .orElseThrow(() -> new NaoEncontradoException("convite-acesso"));
        return new MagicLinkTokenResponse(convite.token());
    }

    @Get(value = "/tokens-conta/{email}/{finalidade}", produces = MediaType.APPLICATION_JSON)
    public MagicLinkTokenResponse latestTokenConta(@PathVariable String email, @PathVariable TokenContaFinalidade finalidade) {
        var tokenConta = tokenContaRepository.findMostRecentByEmailAndFinalidade(new Email(email), finalidade)
            .orElseThrow(() -> new NaoEncontradoException("token-conta"));
        return new MagicLinkTokenResponse(tokenConta.token());
    }

    @Serdeable
    public record MagicLinkTokenResponse(String token) {}
}
