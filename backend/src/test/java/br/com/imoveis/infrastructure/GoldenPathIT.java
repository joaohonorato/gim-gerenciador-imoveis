package br.com.imoveis.infrastructure;

import io.micronaut.core.type.Argument;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest(environments = "test")
class GoldenPathIT {

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    void proprietarioCadastraImovel_geraConvite_inquilinoCandidata_aprovaEAssinatura_geraPagamentos() {
        String ownerToken = login("owner@golden.com", "Owner", "12345678909");
        String ownerBearer = "Bearer " + ownerToken;

        Map<String, Object> imovel = post("/imoveis", ownerBearer,
            Map.of("endereco", "Rua A, 100", "cidade", "SP", "matricula", "M-1"));
        UUID imovelId = UUID.fromString((String) imovel.get("id"));

        Map<String, Object> convite = post("/imoveis/" + imovelId + "/convites", ownerBearer, Map.of(
            "tipoContrato", "RESIDENCIAL",
            "valorAluguel", 1500,
            "dataInicio", LocalDate.of(2026, 1, 1).toString(),
            "dataFim", LocalDate.of(2027, 1, 1).toString(),
            "garantiaAceita", "CAUCAO"));
        String conviteToken = (String) convite.get("token");

        Map<String, Object> candidatura = post("/convites/" + conviteToken + "/cadastro", null, Map.of(
            "username", "inquilino-golden", "cpf", "39053344705", "email", "inquilino@golden.com", "senha", "Senha1234"));
        UUID candidaturaId = UUID.fromString((String) candidatura.get("id"));

        post("/convites/" + conviteToken + "/garantia", null,
            Map.of("tipo", "CAUCAO", "dadosEspecificos", "{\"valor\":3000}"));

        Map<String, Object> contrato = post("/candidaturas/" + candidaturaId + "/aprovar", ownerBearer, Map.of());
        UUID contratoId = UUID.fromString((String) contrato.get("id"));
        assertThat(contrato.get("statusAssinatura")).isEqualTo("PENDENTE");

        Map<String, Object> apos1a = post("/contratos/" + contratoId + "/assinar", ownerBearer,
            Map.of("parte", "PROPRIETARIO"));
        assertThat(apos1a.get("assinouProprietario")).isEqualTo(Boolean.TRUE);
        assertThat(apos1a.get("statusAssinatura")).isEqualTo("PENDENTE");

        Map<String, Object> apos2a = post("/convites/" + conviteToken + "/assinar", null, Map.of());
        assertThat(apos2a.get("assinouInquilino")).isEqualTo(Boolean.TRUE);
        assertThat(apos2a.get("statusAssinatura")).isEqualTo("ASSINADO");

        List<Map<String, Object>> pagamentos = client.toBlocking().retrieve(
            HttpRequest.GET("/contratos/" + contratoId + "/pagamentos").header("Authorization", ownerBearer),
            Argument.listOf(Argument.mapOf(String.class, Object.class)));
        assertThat(pagamentos).hasSize(12);
        assertThat(pagamentos.get(0).get("status")).isEqualTo("PENDENTE");
    }

    @Test
    void proprietarioNaoVeImovelDeOutro() {
        String tokenA = login("a@iso.com", "PropA", "52998224725");
        String tokenB = login("b@iso.com", "PropB", "11144477735");

        Map<String, Object> imovel = post("/imoveis", "Bearer " + tokenA,
            Map.of("endereco", "Rua Iso", "cidade", "SP", "matricula", "M-iso"));
        UUID imovelId = UUID.fromString((String) imovel.get("id"));

        try {
            client.toBlocking().exchange(
                HttpRequest.GET("/imoveis/" + imovelId).header("Authorization", "Bearer " + tokenB),
                Map.class);
            org.junit.jupiter.api.Assertions.fail("expected 4xx");
        } catch (HttpClientResponseException e) {
            assertThat(e.getStatus().getCode()).isBetween(400, 499);
        }
    }

    @Test
    void inquilinoAssinaPublicamentePeloTokenDoConvite() {
        String ownerToken = login("owner-public-sign@test.com", "Owner Public", "39053344705");
        String ownerBearer = "Bearer " + ownerToken;

        Map<String, Object> imovel = post("/imoveis", ownerBearer,
            Map.of("endereco", "Rua Pública, 100", "cidade", "SP", "matricula", "M-2"));
        UUID imovelId = UUID.fromString((String) imovel.get("id"));

        Map<String, Object> convite = post("/imoveis/" + imovelId + "/convites", ownerBearer, Map.of(
            "tipoContrato", "RESIDENCIAL",
            "valorAluguel", 1800,
            "dataInicio", LocalDate.of(2026, 2, 1).toString(),
            "dataFim", LocalDate.of(2027, 2, 1).toString(),
            "garantiaAceita", "CAUCAO"));
        String conviteToken = (String) convite.get("token");

        Map<String, Object> candidatura = post("/convites/" + conviteToken + "/cadastro", null, Map.of(
            "username", "inquilino-publico", "cpf", "39053344705", "email", "inquilino-public@test.com", "senha", "Senha1234"));
        UUID candidaturaId = UUID.fromString((String) candidatura.get("id"));

        post("/convites/" + conviteToken + "/garantia", null,
            Map.of("tipo", "CAUCAO", "dadosEspecificos", "{\"valor\":3600}"));

        Map<String, Object> contrato = post("/candidaturas/" + candidaturaId + "/aprovar", ownerBearer, Map.of());
        UUID contratoId = UUID.fromString((String) contrato.get("id"));

        Map<String, Object> aposProprietario = post("/contratos/" + contratoId + "/assinar", ownerBearer,
            Map.of("parte", "PROPRIETARIO"));
        assertThat(aposProprietario.get("assinouProprietario")).isEqualTo(Boolean.TRUE);

        Map<String, Object> aposInquilino = post("/convites/" + conviteToken + "/assinar", null, Map.of());
        assertThat(aposInquilino.get("assinouInquilino")).isEqualTo(Boolean.TRUE);
        assertThat(aposInquilino.get("statusAssinatura")).isEqualTo("ASSINADO");
    }

    @Test
    void conviteComEmailDeInquilinoExistenteApareceNaInboxSemUsoDeLink() {
        String ownerToken = login("owner-inbox@test.com", "Owner Inbox", gerarCpfValido());
        String ownerBearer = "Bearer " + ownerToken;
        String tenantEmail = "inquilino-inbox@test.com";
        String tenantSenha = "Senha1234";

        Map<String, Object> imovel = post("/imoveis", ownerBearer,
            Map.of("endereco", "Rua Inbox, 100", "cidade", "SP", "matricula", "M-INBOX"));
        UUID imovelId = UUID.fromString((String) imovel.get("id"));

        Map<String, Object> conviteCadastro = post("/imoveis/" + imovelId + "/convites", ownerBearer, Map.of(
            "tipoContrato", "RESIDENCIAL",
            "valorAluguel", 1400,
            "dataInicio", LocalDate.of(2026, 3, 1).toString(),
            "dataFim", LocalDate.of(2027, 3, 1).toString(),
            "garantiaAceita", "CAUCAO"));

        post("/convites/" + conviteCadastro.get("token") + "/cadastro", null, Map.of(
            "username", "inq-inbox", "cpf", gerarCpfValido(), "email", tenantEmail, "senha", tenantSenha));

        Map<String, Object> conviteAutoVinculado = post("/imoveis/" + imovelId + "/convites", ownerBearer, Map.of(
            "tipoContrato", "RESIDENCIAL",
            "valorAluguel", 1700,
            "dataInicio", LocalDate.of(2026, 4, 1).toString(),
            "dataFim", LocalDate.of(2027, 4, 1).toString(),
            "garantiaAceita", "CAUCAO",
            "emailInquilino", tenantEmail));

        Map<?, ?> loginInquilino = client.toBlocking().retrieve(HttpRequest.POST("/auth/login",
            Map.of("email", tenantEmail, "senha", tenantSenha)), Map.class);
        String tenantBearer = "Bearer " + loginInquilino.get("sessionToken");

        List<Map<String, Object>> convites = client.toBlocking().retrieve(
            HttpRequest.GET("/convites/me").header("Authorization", tenantBearer),
            Argument.listOf(Argument.mapOf(String.class, Object.class)));

        String tokenEsperado = (String) conviteAutoVinculado.get("token");
        assertThat(convites).anySatisfy(item -> {
            assertThat(item.get("token")).isEqualTo(tokenEsperado);
            assertThat(item.get("candidaturaStatus")).isEqualTo("PENDENTE");
        });
    }

    @Test
    void proprietarioPodeEscolherCanalWhatsappParaEnvioDeConvite() {
        String ownerToken = login("owner-whatsapp@test.com", "Owner Whats", gerarCpfValido());
        String ownerBearer = "Bearer " + ownerToken;

        Map<String, Object> imovel = post("/imoveis", ownerBearer,
            Map.of("endereco", "Rua Canal, 1", "cidade", "SP", "matricula", "M-CANAL"));
        UUID imovelId = UUID.fromString((String) imovel.get("id"));

        Map<String, Object> convite = post("/imoveis/" + imovelId + "/convites", ownerBearer, Map.of(
            "tipoContrato", "RESIDENCIAL",
            "valorAluguel", 1300,
            "dataInicio", LocalDate.of(2026, 6, 1).toString(),
            "dataFim", LocalDate.of(2027, 6, 1).toString(),
            "garantiaAceita", "CAUCAO",
            "canalEnvio", "WHATSAPP",
            "telefoneInquilino", "11999998888"));

        @SuppressWarnings("unchecked")
        Map<String, Object> envio = (Map<String, Object>) convite.get("envio");
        assertThat(envio.get("canal")).isEqualTo("WHATSAPP");
        assertThat(envio.get("status")).isEqualTo("PRONTO_PARA_ENVIO");
        assertThat(envio.get("whatsappShareUrl")).asString().contains("wa.me");
    }

    @Test
    void proprietarioPodeReenviarConviteComTrocaDeCanalSemRecriar() {
        String ownerToken = login("owner-reenvio@test.com", "Owner Reenvio", gerarCpfValido());
        String ownerBearer = "Bearer " + ownerToken;

        Map<String, Object> imovel = post("/imoveis", ownerBearer,
            Map.of("endereco", "Rua Reenvio, 2", "cidade", "SP", "matricula", "M-REENVIO"));
        UUID imovelId = UUID.fromString((String) imovel.get("id"));

        Map<String, Object> convite = post("/imoveis/" + imovelId + "/convites", ownerBearer, Map.of(
            "tipoContrato", "RESIDENCIAL",
            "valorAluguel", 1300,
            "dataInicio", LocalDate.of(2026, 7, 1).toString(),
            "dataFim", LocalDate.of(2027, 7, 1).toString(),
            "garantiaAceita", "CAUCAO",
            "canalEnvio", "WHATSAPP",
            "telefoneInquilino", "11988887777"));

        String token = (String) convite.get("token");

        Map<String, Object> reenviado = post("/convites/" + token + "/reenviar", ownerBearer, Map.of(
            "canalEnvio", "EMAIL",
            "emailInquilino", "destino-reenvio@test.com"));

        assertThat(reenviado.get("token")).isEqualTo(token);
        @SuppressWarnings("unchecked")
        Map<String, Object> envio = (Map<String, Object>) reenviado.get("envio");
        assertThat(envio.get("canal")).isEqualTo("EMAIL");
        assertThat(envio.get("status")).isIn("ENVIADO", "FALHA", "PULADO");
        assertThat(envio.get("tentativas")).isEqualTo(2);
    }

    @Test
    void conviteSemGarantiaPermiteAprovacaoSemEnvioDeGarantia() {
        String ownerToken = login("owner-sem-garantia@test.com", "Owner Sem Garantia", gerarCpfValido());
        String ownerBearer = "Bearer " + ownerToken;

        Map<String, Object> imovel = post("/imoveis", ownerBearer,
            Map.of("endereco", "Rua Sem Garantia, 10", "cidade", "SP", "matricula", "M-SEM-GARANTIA"));
        UUID imovelId = UUID.fromString((String) imovel.get("id"));

        Map<String, Object> convite = post("/imoveis/" + imovelId + "/convites", ownerBearer, Map.of(
            "tipoContrato", "RESIDENCIAL",
            "valorAluguel", 2100,
            "dataInicio", LocalDate.of(2026, 10, 1).toString(),
            "dataFim", LocalDate.of(2027, 10, 1).toString(),
            "garantiaAceita", "NENHUMA"));
        String conviteToken = (String) convite.get("token");

        Map<String, Object> candidatura = post("/convites/" + conviteToken + "/cadastro", null, Map.of(
            "username", "inq-sem-garantia", "cpf", gerarCpfValido(), "email", "inq-sem-garantia@test.com", "senha", "Senha1234"));
        UUID candidaturaId = UUID.fromString((String) candidatura.get("id"));

        Map<String, Object> contrato = post("/candidaturas/" + candidaturaId + "/aprovar", ownerBearer, Map.of());
        assertThat(contrato.get("garantiaTipo")).isNull();
    }

    @Test
    void proprietarioPodeFiltrarImoveisPorQueryCidadeStatusTipoEBusca() {
        String ownerToken = login("owner-filtro-imovel@test.com", "Owner Filtro", gerarCpfValido());
        String ownerBearer = "Bearer " + ownerToken;

        post("/imoveis", ownerBearer, Map.of(
            "endereco", "Rua das Flores",
            "numero", "120",
            "bairro", "Centro",
            "complemento", "Casa 1",
            "cidade", "Campinas",
            "matricula", "FILTRO-1",
            "tipoImovel", "CASA"));

        post("/imoveis", ownerBearer, Map.of(
            "endereco", "Avenida Paulista",
            "numero", "700",
            "bairro", "Bela Vista",
            "cidade", "Sao Paulo",
            "matricula", "FILTRO-2",
            "tipoImovel", "APARTAMENTO"));

        List<Map<String, Object>> filtrados = getList(
            "/imoveis?cidade=Campinas&status=VAGO&tipoImovel=CASA&busca=flores",
            ownerBearer);

        assertThat(filtrados).hasSize(1);
        assertThat(filtrados.get(0).get("matricula")).isEqualTo("FILTRO-1");
        assertThat(filtrados.get(0).get("tipoImovel")).isEqualTo("CASA");
    }

    private String login(String email, String nome, String cpfCnpj) {
        Map<?, ?> convite = client.toBlocking().retrieve(HttpRequest.POST("/auth/convites/proprietarios",
            Map.of("email", email, "nome", nome, "cpfCnpj", cpfCnpj)), Map.class);
        Map<?, ?> aceite = client.toBlocking().retrieve(HttpRequest.POST(
            "/auth/convites/" + convite.get("token") + "/aceitar",
            Map.of("email", email, "senha", "Senha1234")), Map.class);
        return (String) aceite.get("sessionToken");
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> post(String path, String bearer, Object body) {
        MutableHttpRequest<Object> req = HttpRequest.POST(path, body);
        if (bearer != null) req.header("Authorization", bearer);
        return client.toBlocking().retrieve(req, Map.class);
    }

    private List<Map<String, Object>> getList(String path, String bearer) {
        MutableHttpRequest<Object> req = HttpRequest.GET(path);
        if (bearer != null) req.header("Authorization", bearer);
        return client.toBlocking().retrieve(req, Argument.listOf(Argument.mapOf(String.class, Object.class)));
    }
}
