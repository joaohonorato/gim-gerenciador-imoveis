package br.com.imoveis.application.ports;

public interface PushNotificationSender {
    void enviar(String expoPushToken, String titulo, String corpo);
}
