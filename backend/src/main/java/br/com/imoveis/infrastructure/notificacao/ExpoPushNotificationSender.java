package br.com.imoveis.infrastructure.notificacao;

import br.com.imoveis.application.ports.PushNotificationSender;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

// Envia via Expo Push Service (https://exp.host/--/api/v2/push/send), que
// roteia pra FCM/APNs por baixo — o backend nunca fala direto com
// Firebase/Apple, só com o token Expo já resolvido no device (ver
// frontend/src/api/pushNotifications.ts, getExpoPushTokenAsync). Não exige
// chave de API pra envio básico (só pra recursos avançados de segurança,
// fora de escopo aqui).
@Singleton
public class ExpoPushNotificationSender implements PushNotificationSender {

    private static final Logger log = LoggerFactory.getLogger(ExpoPushNotificationSender.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final URI EXPO_PUSH_URL = URI.create("https://exp.host/--/api/v2/push/send");

    private final HttpClient httpClient;

    public ExpoPushNotificationSender() {
        this.httpClient = HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build();
    }

    @Override
    public void enviar(String expoPushToken, String titulo, String corpo) {
        if (expoPushToken == null || expoPushToken.isBlank()) {
            return;
        }

        String payload = "{" +
            "\"to\":\"" + escapeJson(expoPushToken) + "\"," +
            "\"title\":\"" + escapeJson(titulo) + "\"," +
            "\"body\":\"" + escapeJson(corpo) + "\"," +
            "\"sound\":\"default\"" +
            "}";

        try {
            HttpRequest request = HttpRequest.newBuilder(EXPO_PUSH_URL)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .timeout(REQUEST_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Falha no envio de push via Expo. status={}, body={}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.warn("Erro ao enviar push notification", e);
        }
    }

    private static String escapeJson(String value) {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r");
    }
}
