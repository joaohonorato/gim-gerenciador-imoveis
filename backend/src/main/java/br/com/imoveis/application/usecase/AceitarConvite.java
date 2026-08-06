package br.com.imoveis.application.usecase;

import br.com.imoveis.application.exception.ConflitoException;
import br.com.imoveis.application.exception.NaoEncontradoException;
import br.com.imoveis.application.ports.Clock;
import br.com.imoveis.application.ports.ContaAcessoRepository;
import br.com.imoveis.application.ports.ConviteRepository;
import br.com.imoveis.application.ports.InquilinoRepository;
import br.com.imoveis.application.ports.PasswordHasher;
import br.com.imoveis.domain.auditoria.EntidadeAuditoria;
import br.com.imoveis.domain.auditoria.TipoEventoAuditoria;
import br.com.imoveis.domain.auth.ContaAcesso;
import br.com.imoveis.domain.convite.Candidatura;
import br.com.imoveis.domain.convite.Convite;
import br.com.imoveis.domain.inquilino.Inquilino;
import br.com.imoveis.domain.shared.Cpf;
import br.com.imoveis.domain.shared.Email;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;

@Singleton
@Transactional
public class AceitarConvite {

    private final ConviteRepository conviteRepository;
    private final InquilinoRepository inquilinoRepository;
    private final ContaAcessoRepository contaAcessoRepository;
    private final PasswordHasher passwordHasher;
    private final RegistrarEventoAuditoria registrarEventoAuditoria;
    private final Clock clock;

    public AceitarConvite(ConviteRepository conviteRepository,
                           InquilinoRepository inquilinoRepository,
                           ContaAcessoRepository contaAcessoRepository,
                           PasswordHasher passwordHasher,
                           RegistrarEventoAuditoria registrarEventoAuditoria,
                           Clock clock) {
        this.conviteRepository = conviteRepository;
        this.inquilinoRepository = inquilinoRepository;
        this.contaAcessoRepository = contaAcessoRepository;
        this.passwordHasher = passwordHasher;
        this.registrarEventoAuditoria = registrarEventoAuditoria;
        this.clock = clock;
    }

    public Result execute(String token, String username, String cpf, String email, String senha) {
        Convite convite = conviteRepository.findByToken(token)
            .orElseThrow(() -> new NaoEncontradoException("convite"));
        if (convite.expirado(clock.now())) {
            registrarEventoAuditoria.execute(EntidadeAuditoria.CONVITE, convite.id(),
                TipoEventoAuditoria.TENTATIVA_COM_TOKEN_EXPIRADO, null);
            throw new ConflitoException("convite expirado");
        }

        Cpf cpfObj = new Cpf(cpf);
        Email emailObj = new Email(email);
        Inquilino inquilino = inquilinoRepository.findByCpf(cpfObj)
            .orElseGet(() -> inquilinoRepository.save(
                Inquilino.cadastrar(username, cpfObj, emailObj, clock.now())));

        contaAcessoRepository.findByEmail(emailObj).ifPresent(conta -> {
            throw new ConflitoException("já existe conta cadastrada para este e-mail");
        });
        contaAcessoRepository.save(
            ContaAcesso.vincularInquilino(emailObj, passwordHasher.hash(senha), inquilino.id(), clock.now()));

        Candidatura candidatura = Candidatura.nova(convite.id(), inquilino.id(), clock.now());
        candidatura = conviteRepository.saveCandidatura(candidatura);

        convite.marcarCandidaturaCriada(candidatura.id());
        conviteRepository.save(convite);
        registrarEventoAuditoria.execute(EntidadeAuditoria.CONVITE, convite.id(),
            TipoEventoAuditoria.CANDIDATURA_CRIADA, "cadastro novo de inquilino");

        return new Result(convite, candidatura, inquilino);
    }

    public record Result(Convite convite, Candidatura candidatura, Inquilino inquilino) {}
}
