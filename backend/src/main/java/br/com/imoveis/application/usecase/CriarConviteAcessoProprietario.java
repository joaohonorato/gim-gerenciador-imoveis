package br.com.imoveis.application.usecase;

import br.com.imoveis.application.exception.ConflitoException;
import br.com.imoveis.application.ports.Clock;
import br.com.imoveis.application.ports.ContaAcessoRepository;
import br.com.imoveis.application.ports.ConviteAcessoRepository;
import br.com.imoveis.domain.auditoria.EntidadeAuditoria;
import br.com.imoveis.domain.auditoria.TipoEventoAuditoria;
import br.com.imoveis.domain.auth.ConviteAcesso;
import br.com.imoveis.domain.proprietario.PerfilProprietario;
import br.com.imoveis.domain.shared.Email;
import jakarta.inject.Singleton;

import java.util.Optional;

@Singleton
public class CriarConviteAcessoProprietario {

    private final ConviteAcessoRepository conviteAcessoRepository;
    private final ContaAcessoRepository contaAcessoRepository;
    private final RegistrarEventoAuditoria registrarEventoAuditoria;
    private final Clock clock;

    public CriarConviteAcessoProprietario(ConviteAcessoRepository conviteAcessoRepository,
                                         ContaAcessoRepository contaAcessoRepository,
                                         RegistrarEventoAuditoria registrarEventoAuditoria,
                                         Clock clock) {
        this.conviteAcessoRepository = conviteAcessoRepository;
        this.contaAcessoRepository = contaAcessoRepository;
        this.registrarEventoAuditoria = registrarEventoAuditoria;
        this.clock = clock;
    }

    public ConviteAcesso execute(String email, String nome, String cpfCnpj, PerfilProprietario perfil) {
        Email targetEmail = new Email(email);
        if (contaAcessoRepository.findByEmail(targetEmail).isPresent()) {
            throw new ConflitoException("já existe conta cadastrada para este e-mail");
        }

        // Chamar este endpoint de novo pro mesmo e-mail (link perdido, pedido
        // de reenvio) não pode simplesmente empilhar um convite novo — isso
        // deixa tokens antigos órfãos e ambíguos sobre qual link é "o
        // válido". Se já existe um convite pendente (não consumido e ainda
        // dentro do prazo) pra esse e-mail, devolve o mesmo em vez de criar
        // outro.
        Optional<ConviteAcesso> existente = conviteAcessoRepository.findMostRecentByEmail(targetEmail);
        if (existente.isPresent() && !existente.get().consumido() && !existente.get().expirado(clock.now())) {
            return existente.get();
        }

        ConviteAcesso convite = ConviteAcesso.convidarProprietario(targetEmail, nome, cpfCnpj, perfil, clock.now());
        convite = conviteAcessoRepository.save(convite);
        registrarEventoAuditoria.execute(EntidadeAuditoria.CONVITE_ACESSO, convite.id(), TipoEventoAuditoria.CRIADO, null);
        return convite;
    }
}