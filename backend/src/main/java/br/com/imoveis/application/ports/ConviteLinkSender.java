package br.com.imoveis.application.ports;

public interface ConviteLinkSender {

    ResultadoEnvio enviarPorEmail(String emailDestino, String token);

    ResultadoEnvio enviarPorWhatsapp(String telefoneDestino, String token);

    record ResultadoEnvio(String status, String destino, String whatsappShareUrl, String detalhe) {}
}
