package br.com.imoveis.application.usecase;

import br.com.imoveis.application.ports.ConviteLinkSender;
import br.com.imoveis.application.ports.ConviteRepository;
import br.com.imoveis.application.ports.Clock;
import br.com.imoveis.domain.convite.CanalEnvioConvite;
import br.com.imoveis.domain.convite.Convite;
import br.com.imoveis.domain.convite.ConviteStatus;
import jakarta.inject.Singleton;

@Singleton
public class EnviarLinkConvite {

    private final ConviteLinkSender conviteLinkSender;
    private final ConviteRepository conviteRepository;
    private final Clock clock;

    public EnviarLinkConvite(ConviteLinkSender conviteLinkSender,
                             ConviteRepository conviteRepository,
                             Clock clock) {
        this.conviteLinkSender = conviteLinkSender;
        this.conviteRepository = conviteRepository;
        this.clock = clock;
    }

    public ConviteLinkSender.ResultadoEnvio execute(Convite convite,
                                                    CanalEnvioConvite canal,
                                                    String emailDestino,
                                                    String telefoneDestino) {
        if (canal == null) {
            return null;
        }

        ConviteLinkSender.ResultadoEnvio resultado;
        if (convite.status() != ConviteStatus.PENDENTE) {
            resultado = new ConviteLinkSender.ResultadoEnvio(
                "PULADO",
                null,
                null,
                "Convite já foi vinculado automaticamente para um inquilino existente");
            registrarAuditoria(convite, canal, resultado);
            return resultado;
        }

        resultado = switch (canal) {
            case EMAIL -> {
                if (emailDestino == null || emailDestino.isBlank()) {
                    throw new IllegalArgumentException("email do inquilino é obrigatório para envio por e-mail");
                }
                yield conviteLinkSender.enviarPorEmail(emailDestino, convite.token());
            }
            case WHATSAPP -> {
                if (telefoneDestino == null || telefoneDestino.isBlank()) {
                    throw new IllegalArgumentException("telefone do inquilino é obrigatório para envio por WhatsApp");
                }
                yield conviteLinkSender.enviarPorWhatsapp(telefoneDestino, convite.token());
            }
        };
        registrarAuditoria(convite, canal, resultado);
        return resultado;
    }

    private void registrarAuditoria(Convite convite,
                                    CanalEnvioConvite canal,
                                    ConviteLinkSender.ResultadoEnvio resultado) {
        convite.registrarEnvio(
            canal,
            resultado.status(),
            resultado.destino(),
            resultado.whatsappShareUrl(),
            resultado.detalhe(),
            clock.now());
        conviteRepository.save(convite);
    }
}
