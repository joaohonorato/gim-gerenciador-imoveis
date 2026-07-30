package br.com.imoveis.application.usecase;

import br.com.imoveis.application.exception.ConflitoException;
import br.com.imoveis.application.ports.Clock;
import br.com.imoveis.application.ports.ContaAcessoRepository;
import br.com.imoveis.application.ports.ConviteAcessoRepository;
import br.com.imoveis.domain.auth.ConviteAcesso;
import br.com.imoveis.domain.proprietario.PerfilProprietario;
import br.com.imoveis.domain.shared.Email;
import jakarta.inject.Singleton;

@Singleton
public class CriarConviteAcessoProprietario {

    private final ConviteAcessoRepository conviteAcessoRepository;
    private final ContaAcessoRepository contaAcessoRepository;
    private final Clock clock;

    public CriarConviteAcessoProprietario(ConviteAcessoRepository conviteAcessoRepository,
                                         ContaAcessoRepository contaAcessoRepository,
                                         Clock clock) {
        this.conviteAcessoRepository = conviteAcessoRepository;
        this.contaAcessoRepository = contaAcessoRepository;
        this.clock = clock;
    }

    public ConviteAcesso execute(String email, String nome, String cpfCnpj, PerfilProprietario perfil) {
        Email targetEmail = new Email(email);
        if (contaAcessoRepository.findByEmail(targetEmail).isPresent()) {
            throw new ConflitoException("já existe conta cadastrada para este e-mail");
        }
        ConviteAcesso convite = ConviteAcesso.convidarProprietario(targetEmail, nome, cpfCnpj, perfil, clock.now());
        return conviteAcessoRepository.save(convite);
    }
}