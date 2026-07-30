package br.com.imoveis.infrastructure.convite;

import br.com.imoveis.application.ports.ConviteLinkSender;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Singleton
public class ResendConviteLinkSender implements ConviteLinkSender {

    private static final Logger log = LoggerFactory.getLogger(ResendConviteLinkSender.class);

    private final HttpClient httpClient;
    private final String resendApiKey;
    private final String resendFromEmail;
    private final String frontendBaseUrl;

    public ResendConviteLinkSender(
        @Value("${app.convites.resend.api-key:}") String resendApiKey,
        @Value("${app.convites.resend.from-email:no-reply@imoveis.local}") String resendFromEmail,
        @Value("${app.convites.frontend-base-url:http://localhost:19006}") String frontendBaseUrl
    ) {
        this.httpClient = HttpClient.newHttpClient();
        this.resendApiKey = resendApiKey;
        this.resendFromEmail = resendFromEmail;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Override
    public ResultadoEnvio enviarPorEmail(String emailDestino, String token) {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            log.warn("Resend não configurado; envio de convite por e-mail foi pulado para {}", emailDestino);
            return new ResultadoEnvio("PULADO", emailDestino, null, "Configure app.convites.resend.api-key para envio real");
        }

        String link = conviteLink(token);
        String payload = "{" +
            "\"from\":\"" + escapeJson(resendFromEmail) + "\"," +
            "\"to\":[\"" + escapeJson(emailDestino) + "\"]," +
            "\"subject\":\"Convite para locação\"," +
            "\"html\":\"<p>Você recebeu um convite de locação.</p><p><a href=\\\"" + escapeJson(link) + "\\\">Abrir convite</a></p>\"," +
            "\"text\":\"Você recebeu um convite de locação. Abra: " + escapeJson(link) + "\"" +
            "}";

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.resend.com/emails"))
                .header("Authorization", "Bearer " + resendApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return new ResultadoEnvio("ENVIADO", emailDestino, null, "E-mail enviado com sucesso");
            }

            log.warn("Falha no envio Resend. status={}, body={}", response.statusCode(), response.body());
            return new ResultadoEnvio("FALHA", emailDestino, null, "Falha ao enviar e-mail via Resend");
        } catch (Exception e) {
            log.warn("Erro ao enviar convite por e-mail", e);
            return new ResultadoEnvio("FALHA", emailDestino, null, "Erro ao enviar e-mail: " + e.getMessage());
        }
    }

    @Override
    public ResultadoEnvio enviarPorWhatsapp(String telefoneDestino, String token) {
        String link = conviteLink(token);
        String mensagem = "Você recebeu um convite de locação. Abra: " + link;
        String whatsappUrl = "https://wa.me/" + sanitizePhone(telefoneDestino)
            + "?text=" + URLEncoder.encode(mensagem, StandardCharsets.UTF_8);
        return new ResultadoEnvio("PRONTO_PARA_ENVIO", telefoneDestino, whatsappUrl, "Abra o link para concluir o envio no WhatsApp");
    }

    private String conviteLink(String token) {
        String base = frontendBaseUrl.endsWith("/") ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1) : frontendBaseUrl;
        return base + "/locacao/" + token;
    }

    private static String sanitizePhone(String telefone) {
        return telefone == null ? "" : telefone.replaceAll("\\D", "");
    }

    private static String escapeJson(String value) {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r");
    }
}
