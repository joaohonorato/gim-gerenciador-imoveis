package br.com.imoveis.infrastructure;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@MicronautTest(environments = "test")
class AuthFlowIT {

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    void sem_token_retorna_401() {
        var ex = assertThrows(HttpClientResponseException.class,
            () -> client.toBlocking().exchange(HttpRequest.GET("/imoveis")));
        assertThat(ex.getStatus().getCode()).isEqualTo(HttpStatus.UNAUTHORIZED.getCode());
    }

    @Test
    void token_invalido_retorna_401() {
        var req = HttpRequest.GET("/imoveis").bearerAuth("token-inexistente-xyz");
        var ex = assertThrows(HttpClientResponseException.class,
            () -> client.toBlocking().exchange(req));
        assertThat(ex.getStatus().getCode()).isEqualTo(HttpStatus.UNAUTHORIZED.getCode());
    }

    @Test
    void rota_publica_nao_exige_auth() {
        String cpf = gerarCpfValido();
        var res = client.toBlocking().exchange(HttpRequest.POST("/auth/register/proprietario",
            Map.of("email", "auth-publico@test.com", "senha", "Senha1234", "username", "Pub", "cpfCnpj", cpf)));
        assertThat(res.getStatus().getCode()).isBetween(200, 299);
    }

    @Test
    void proprietario_pode_se_registrar_diretamente() {
        Map<?, ?> cadastro = client.toBlocking().retrieve(HttpRequest.POST("/auth/register/proprietario",
            Map.of("email", "owner-register@test.com", "senha", "Senha1234", "username", "owner-register", "cpfCnpj", "06435489980")), Map.class);
        assertThat(cadastro.get("sessionToken")).isNotNull();

        Map<?, ?> login = client.toBlocking().retrieve(HttpRequest.POST("/auth/login",
            Map.of("email", "owner-register@test.com", "senha", "Senha1234")), Map.class);
        assertThat(login.get("sessionToken")).isNotNull();
    }

    @Test
    void convite_aceito_permite_login_com_email_e_senha() {
        Map<?, ?> convite = client.toBlocking().retrieve(HttpRequest.POST("/auth/convites/proprietarios",
            Map.of("email", "owner-auth@test.com", "nome", "Owner Auth", "cpfCnpj", "12345679034")), Map.class);

        Map<?, ?> aceite = client.toBlocking().retrieve(HttpRequest.POST(
            "/auth/convites/" + convite.get("token") + "/aceitar",
            Map.of("email", "owner-auth@test.com", "senha", "Senha1234")), Map.class);
        assertThat(aceite.get("sessionToken")).isNotNull();

        Map<?, ?> login = client.toBlocking().retrieve(HttpRequest.POST("/auth/login",
            Map.of("email", "owner-auth@test.com", "senha", "Senha1234")), Map.class);
        assertThat(login.get("sessionToken")).isNotNull();
    }

    @Test
    void logout_invalida_token_da_sessao_atual() {
        String cpf = gerarCpfValido();
        Map<?, ?> cadastro = client.toBlocking().retrieve(HttpRequest.POST("/auth/register/proprietario",
            Map.of("email", "owner-logout@test.com", "senha", "Senha1234", "username", "owner-logout", "cpfCnpj", cpf)), Map.class);

        String token = (String) cadastro.get("sessionToken");

        client.toBlocking().exchange(HttpRequest.POST("/auth/logout", "").bearerAuth(token));

        var ex = assertThrows(HttpClientResponseException.class,
            () -> client.toBlocking().exchange(HttpRequest.GET("/auth/me").bearerAuth(token)));
        assertThat(ex.getStatus().getCode()).isEqualTo(HttpStatus.UNAUTHORIZED.getCode());
    }

    private static String gerarCpfValido() {
        int[] digits = new int[11];
        for (int i = 0; i < 9; i++) {
            digits[i] = ThreadLocalRandom.current().nextInt(0, 10);
        }
        digits[9] = calcularDigitoCpf(digits, 9, 10);
        digits[10] = calcularDigitoCpf(digits, 10, 11);

        StringBuilder cpf = new StringBuilder(11);
        for (int i = 0; i < 11; i++) {
            cpf.append(digits[i]);
        }
        return cpf.toString();
    }

    private static int calcularDigitoCpf(int[] digits, int length, int weightStart) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += digits[i] * (weightStart - i);
        }
        int mod = (sum * 10) % 11;
        return mod == 10 ? 0 : mod;
    }
}
