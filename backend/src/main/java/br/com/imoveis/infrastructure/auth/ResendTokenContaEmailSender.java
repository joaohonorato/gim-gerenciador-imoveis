package br.com.imoveis.infrastructure.auth;

import br.com.imoveis.application.ports.TokenContaEmailSender;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Singleton
public class ResendTokenContaEmailSender implements TokenContaEmailSender {

    private static final Logger log = LoggerFactory.getLogger(ResendTokenContaEmailSender.class);

    private final HttpClient httpClient;
    private final String resendApiKey;
    private final String resendFromEmail;
    private final String frontendBaseUrl;

    public ResendTokenContaEmailSender(
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
    public void enviarResetSenha(String emailDestino, String token) {
        String link = link("redefinir-senha", token);
        enviar(emailDestino, "Redefinição de senha",
            "<p>Recebemos um pedido para redefinir sua senha.</p><p><a href=\"" + escapeJson(link) + "\">Redefinir senha</a></p><p>Se você não pediu isso, ignore este e-mail.</p>",
            "Redefina sua senha em: " + link + " — se você não pediu isso, ignore este e-mail.");
    }

    @Override
    public void enviarVerificacaoEmail(String emailDestino, String token) {
        String link = link("verificar-email", token);
        enviar(emailDestino, "Confirme seu e-mail",
            "<p>Confirme seu e-mail para concluir seu cadastro.</p><p><a href=\"" + escapeJson(link) + "\">Confirmar e-mail</a></p>",
            "Confirme seu e-mail em: " + link);
    }

    private void enviar(String emailDestino, String subject, String html, String text) {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            log.warn("Resend não configurado; envio de e-mail de conta foi pulado para {}", emailDestino);
            return;
        }

        String payload = "{" +
            "\"from\":\"" + escapeJson(resendFromEmail) + "\"," +
            "\"to\":[\"" + escapeJson(emailDestino) + "\"]," +
            "\"subject\":\"" + escapeJson(subject) + "\"," +
            "\"html\":\"" + escapeJson(html) + "\"," +
            "\"text\":\"" + escapeJson(text) + "\"" +
            "}";

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.resend.com/emails"))
                .header("Authorization", "Bearer " + resendApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Falha no envio Resend. status={}, body={}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.warn("Erro ao enviar e-mail de conta", e);
        }
    }

    private String link(String path, String token) {
        String base = frontendBaseUrl.endsWith("/") ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1) : frontendBaseUrl;
        return base + "/" + path + "/" + token;
    }

    private static String escapeJson(String value) {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r");
    }
}
