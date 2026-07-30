package br.com.imoveis.infrastructure;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@MicronautTest(environments = "test")
class RecusarCandidatoIT {

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    void proprietario_alheio_nao_pode_recusar_candidatura() {
        // CPFs válidos únicos: 43214567896, 11122233396, 12332112340
        String tokenA = login("recusar-a@test.com", "PropA", "43214567896");
        String tokenB = login("recusar-b@test.com", "PropB", "11122233396");

        Map<String, Object> imovel = post("/imoveis", "Bearer " + tokenA,
            Map.of("endereco", "Rua Recusar, 1", "cidade", "SP", "matricula", "REC-1"));
        UUID imovelId = UUID.fromString((String) imovel.get("id"));

        Map<String, Object> convite = post("/imoveis/" + imovelId + "/convites", "Bearer " + tokenA, Map.of(
            "tipoContrato", "RESIDENCIAL",
            "valorAluguel", 1000,
            "dataInicio", LocalDate.of(2026, 9, 1).toString(),
            "dataFim", LocalDate.of(2027, 9, 1).toString(),
            "garantiaAceita", "CAUCAO"));
        String conviteToken = (String) convite.get("token");

        Map<String, Object> candidatura = post("/convites/" + conviteToken + "/cadastro", null,
            Map.of("username", "inq-rec-1", "cpf", "12332112340", "email", "rec-inq@test.com", "senha", "Senha1234"));
        UUID candidaturaId = UUID.fromString((String) candidatura.get("id"));

        var ex = assertThrows(HttpClientResponseException.class,
            () -> post("/candidaturas/" + candidaturaId + "/recusar", "Bearer " + tokenB, Map.of()));
        assertThat(ex.getStatus().getCode()).isEqualTo(HttpStatus.NOT_FOUND.getCode());
    }

    @Test
    void proprietario_correto_pode_recusar_candidatura() {
        // CPFs válidos únicos: 22233344405, 12312312387
        String tokenA = login("recusar-c@test.com", "PropC", "22233344405");

        Map<String, Object> imovel = post("/imoveis", "Bearer " + tokenA,
            Map.of("endereco", "Rua Recusar, 2", "cidade", "SP", "matricula", "REC-2"));
        UUID imovelId = UUID.fromString((String) imovel.get("id"));

        Map<String, Object> convite = post("/imoveis/" + imovelId + "/convites", "Bearer " + tokenA, Map.of(
            "tipoContrato", "RESIDENCIAL",
            "valorAluguel", 1000,
            "dataInicio", LocalDate.of(2026, 10, 1).toString(),
            "dataFim", LocalDate.of(2027, 10, 1).toString(),
            "garantiaAceita", "CAUCAO"));
        String conviteToken = (String) convite.get("token");

        Map<String, Object> candidatura = post("/convites/" + conviteToken + "/cadastro", null,
            Map.of("username", "inq-rec-2", "cpf", "12312312387", "email", "rec-inq2@test.com", "senha", "Senha1234"));
        UUID candidaturaId = UUID.fromString((String) candidatura.get("id"));

        client.toBlocking().exchange(
            HttpRequest.POST("/candidaturas/" + candidaturaId + "/recusar", Map.of())
                .header("Authorization", "Bearer " + tokenA));
    }

    private String login(String email, String nome, String cpfCnpj) {
        Map<?, ?> convite = client.toBlocking().retrieve(HttpRequest.POST("/auth/convites/proprietarios",
            Map.of("email", email, "nome", nome, "cpfCnpj", cpfCnpj)), Map.class);
        Map<?, ?> aceite = client.toBlocking().retrieve(HttpRequest.POST(
            "/auth/convites/" + convite.get("token") + "/aceitar",
            Map.of("email", email, "senha", "Senha1234")), Map.class);
        return (String) aceite.get("sessionToken");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> post(String path, String bearer, Object body) {
        MutableHttpRequest<Object> req = HttpRequest.POST(path, body);
        if (bearer != null) req.header("Authorization", bearer);
        return client.toBlocking().retrieve(req, Map.class);
    }
}
