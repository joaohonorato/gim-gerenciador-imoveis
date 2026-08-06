package br.com.imoveis.application.usecase;

import br.com.imoveis.application.ports.Clock;
import br.com.imoveis.application.ports.MagicLinkRepository;
import br.com.imoveis.application.ports.MagicLinkSender;
import br.com.imoveis.application.ports.ProprietarioRepository;
import br.com.imoveis.domain.proprietario.Proprietario;
import br.com.imoveis.domain.shared.CpfCnpj;
import br.com.imoveis.domain.shared.Email;
import jakarta.inject.Singleton;

import java.security.SecureRandom;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Singleton
public class SolicitarMagicLink {

    private static final SecureRandom RNG = new SecureRandom();

    private final MagicLinkRepository repository;
    private final MagicLinkSender sender;
    private final ProprietarioRepository proprietarioRepository;
    private final SemearCatalogosPadrao semearCatalogosPadrao;
    private final Clock clock;

    public SolicitarMagicLink(MagicLinkRepository repository, MagicLinkSender sender,
                               ProprietarioRepository proprietarioRepository,
                               SemearCatalogosPadrao semearCatalogosPadrao, Clock clock) {
        this.repository = repository;
        this.sender = sender;
        this.proprietarioRepository = proprietarioRepository;
        this.semearCatalogosPadrao = semearCatalogosPadrao;
        this.clock = clock;
    }

    public String execute(String rawEmail, String nomeOpcional, String cpfCnpjOpcional) {
        Email email = new Email(rawEmail);

        if (proprietarioRepository.findByEmail(email).isEmpty() && nomeOpcional != null && cpfCnpjOpcional != null) {
            Proprietario novo = Proprietario.cadastrar(nomeOpcional, CpfCnpj.parse(cpfCnpjOpcional), email, clock.now());
            proprietarioRepository.save(novo);
            semearCatalogosPadrao.execute(novo.id());
        }

        String token = geraToken();
        MagicLinkRepository.MagicLink link = new MagicLinkRepository.MagicLink(
            token, email, clock.now().plus(7, ChronoUnit.DAYS), false);
        repository.save(link);
        sender.send(email, token);
        return token;
    }

    private static String geraToken() {
        byte[] bytes = new byte[24];
        RNG.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
