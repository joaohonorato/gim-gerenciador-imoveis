package br.com.imoveis.application.usecase;

import br.com.imoveis.application.exception.AutenticacaoInvalidaException;
import br.com.imoveis.application.ports.Clock;
import br.com.imoveis.application.ports.ContaAcessoRepository;
import br.com.imoveis.application.ports.MagicLinkRepository;
import br.com.imoveis.application.ports.ProprietarioRepository;
import br.com.imoveis.application.ports.SessaoRepository;
import br.com.imoveis.domain.auth.ContaAcesso;
import br.com.imoveis.domain.proprietario.Proprietario;
import jakarta.inject.Singleton;

import java.security.SecureRandom;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Singleton
public class VerificarMagicLink {

    private static final SecureRandom RNG = new SecureRandom();

    private final MagicLinkRepository magicLinkRepository;
    private final ContaAcessoRepository contaAcessoRepository;
    private final ProprietarioRepository proprietarioRepository;
    private final SessaoRepository sessaoRepository;
    private final Clock clock;

    public VerificarMagicLink(MagicLinkRepository magicLinkRepository,
                               ContaAcessoRepository contaAcessoRepository,
                               ProprietarioRepository proprietarioRepository,
                               SessaoRepository sessaoRepository, Clock clock) {
        this.magicLinkRepository = magicLinkRepository;
        this.contaAcessoRepository = contaAcessoRepository;
        this.proprietarioRepository = proprietarioRepository;
        this.sessaoRepository = sessaoRepository;
        this.clock = clock;
    }

    public Result execute(String token) {
        MagicLinkRepository.MagicLink link = magicLinkRepository.findByToken(token)
            .orElseThrow(() -> new AutenticacaoInvalidaException("token inválido"));
        if (link.consumido()) {
            throw new AutenticacaoInvalidaException("token já consumido");
        }
        if (clock.now().isAfter(link.expiraEm())) {
            throw new AutenticacaoInvalidaException("token expirado");
        }
        Proprietario proprietario = proprietarioRepository.findByEmail(link.email())
            .orElseThrow(() -> new AutenticacaoInvalidaException("proprietário não cadastrado"));
        ContaAcesso contaAcesso = contaAcessoRepository.findByEmail(link.email())
            .orElseThrow(() -> new AutenticacaoInvalidaException("conta de acesso não cadastrada"));

        magicLinkRepository.consumir(token);
        String sessionToken = geraSessao();
        sessaoRepository.save(new SessaoRepository.Sessao(
            sessionToken, contaAcesso.id(), contaAcesso.tipo(), contaAcesso.proprietarioId(),
            contaAcesso.inquilinoId(), clock.now().plus(24, ChronoUnit.HOURS)));
        return new Result(sessionToken, proprietario);
    }

    private static String geraSessao() {
        byte[] bytes = new byte[24];
        RNG.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record Result(String sessionToken, Proprietario proprietario) {}
}
