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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Cobre a auditoria de posse por {id}: toda rota que resolve um recurso a
// partir de um path variable precisa filtrar pelo proprietarioId/inquilinoId
// do Principal autenticado, e o resultado de uma tentativa de acesso alheio
// deve ser sempre 404 (NaoEncontradoException) — nunca 200 com dado vazado,
// nem 403 (que confirmaria a existência do recurso pra quem não tem acesso).
@MicronautTest(environments = "test")
class IdorAccessControlIT {

    @Inject
    @Client("/")
    HttpClient client;

    private String ownerABearer;
    private String ownerBBearer;
    private String tenantABearer;
    private String tenantCBearer;

    private UUID imovelAId;
    private UUID contratoAId;
    private UUID inquilinoAId;
    private UUID contaAId;
    private UUID chamadoAId;

    @BeforeEach
    void montarCenario() {
        // @BeforeEach roda antes de cada @Test nesta classe, então os e-mails
        // precisam ser únicos por execução — senão a 2ª invocação colide com
        // conta já cadastrada pela 1ª (schema H2 do profile "test" persiste
        // entre métodos do mesmo run).
        String sufixo = String.valueOf(ThreadLocalRandom.current().nextLong(1_000_000_000L, 9_999_999_999L));
        ownerABearer = "Bearer " + login("idor-owner-a-" + sufixo + "@test.com", "Owner IDOR A", gerarCpfValido());
        ownerBBearer = "Bearer " + login("idor-owner-b-" + sufixo + "@test.com", "Owner IDOR B", gerarCpfValido());

        Map<String, Object> imovelA = post("/imoveis", ownerABearer,
            Map.of("endereco", "Rua IDOR A, 1", "cidade", "SP", "matricula", "IDOR-A-1"));
        imovelAId = UUID.fromString((String) imovelA.get("id"));

        Map<String, Object> categoria = post("/categorias-chamado", ownerABearer, Map.of("nome", "Elétrica IDOR"));
        UUID categoriaId = UUID.fromString((String) categoria.get("id"));

        Map<String, Object> tipoConta = post("/tipos-conta", ownerABearer, Map.of("nome", "IPTU IDOR"));
        UUID tipoContaId = UUID.fromString((String) tipoConta.get("id"));

        Map<String, Object> convite = post("/imoveis/" + imovelAId + "/convites", ownerABearer, Map.of(
            "tipoContrato", "RESIDENCIAL",
            "valorAluguel", 1200,
            "dataInicio", LocalDate.of(2026, 9, 1).toString(),
            "dataFim", LocalDate.of(2027, 9, 1).toString(),
            "garantiaAceita", "CAUCAO"));
        String conviteToken = (String) convite.get("token");

        String tenantAEmail = "idor-tenant-a-" + sufixo + "@test.com";
        String tenantASenha = "Senha1234";
        Map<String, Object> candidatura = post("/convites/" + conviteToken + "/cadastro", null, Map.of(
            "username", "idor-tenant-a", "cpf", gerarCpfValido(), "email", tenantAEmail, "senha", tenantASenha));
        UUID candidaturaId = UUID.fromString((String) candidatura.get("id"));

        tenantABearer = "Bearer " + loginComSenha(tenantAEmail, tenantASenha);

        post("/convites/" + conviteToken + "/garantia", null,
            Map.of("tipo", "CAUCAO", "dadosEspecificos", "{\"valor\":3000}"));

        Map<String, Object> contrato = post("/candidaturas/" + candidaturaId + "/aprovar", ownerABearer, Map.of());
        contratoAId = UUID.fromString((String) contrato.get("id"));
        inquilinoAId = UUID.fromString((String) contrato.get("inquilinoId"));

        post("/contratos/" + contratoAId + "/assinar", ownerABearer, Map.of("parte", "PROPRIETARIO"));
        post("/convites/" + conviteToken + "/assinar", null, Map.of());

        Map<String, Object> conta = post("/imoveis/" + imovelAId + "/contas", ownerABearer, Map.of(
            "tipoContaId", tipoContaId.toString(),
            "vencimento", LocalDate.of(2026, 10, 10).toString(),
            "valor", new BigDecimal("150.00"),
            "responsavel", "PROPRIETARIO"));
        contaAId = UUID.fromString((String) conta.get("id"));

        Map<String, Object> chamado = post("/imoveis/" + imovelAId + "/chamados", tenantABearer, Map.of(
            "categoriaId", categoriaId.toString(), "descricao", "Torneira vazando"));
        chamadoAId = UUID.fromString((String) chamado.get("id"));

        // Inquilino totalmente sem relação com o proprietário A: se candidata a um
        // imóvel do proprietário B mas nunca chega a ter contrato aprovado/assinado.
        Map<String, Object> imovelB = post("/imoveis", ownerBBearer,
            Map.of("endereco", "Rua IDOR B, 1", "cidade", "SP", "matricula", "IDOR-B-1"));
        UUID imovelBId = UUID.fromString((String) imovelB.get("id"));
        Map<String, Object> conviteB = post("/imoveis/" + imovelBId + "/convites", ownerBBearer, Map.of(
            "tipoContrato", "RESIDENCIAL",
            "valorAluguel", 1000,
            "dataInicio", LocalDate.of(2026, 9, 1).toString(),
            "dataFim", LocalDate.of(2027, 9, 1).toString(),
            "garantiaAceita", "CAUCAO"));
        String tenantCEmail = "idor-tenant-c-" + sufixo + "@test.com";
        String tenantCSenha = "Senha1234";
        post("/convites/" + conviteB.get("token") + "/cadastro", null, Map.of(
            "username", "idor-tenant-c", "cpf", gerarCpfValido(), "email", tenantCEmail, "senha", tenantCSenha));
        tenantCBearer = "Bearer " + loginComSenha(tenantCEmail, tenantCSenha);
    }

    @Test
    void proprietarioAlheioNaoVeContratoDeOutro() {
        assertNotFound(() -> getJson("/contratos/" + contratoAId, ownerBBearer));
    }

    @Test
    void proprietarioAlheioNaoVePagamentosDeContratoDeOutro() {
        assertNotFound(() -> getList("/contratos/" + contratoAId + "/pagamentos", ownerBBearer));
    }

    @Test
    void proprietarioAlheioNaoAssinaContratoDeOutro() {
        assertNotFound(() -> post("/contratos/" + contratoAId + "/assinar", ownerBBearer, Map.of("parte", "PROPRIETARIO")));
    }

    @Test
    void proprietarioAlheioNaoVeImovelDeOutro() {
        assertNotFound(() -> getJson("/imoveis/" + imovelAId, ownerBBearer));
    }

    @Test
    void proprietarioAlheioNaoVeInquilinoDeOutro() {
        assertNotFound(() -> getJson("/inquilinos/" + inquilinoAId, ownerBBearer));
    }

    @Test
    void proprietarioAlheioNaoMarcaContaDeOutroComoPaga() {
        assertNotFound(() -> patch("/contas/" + contaAId, ownerBBearer, Map.of()));
    }

    @Test
    void proprietarioAlheioNaoCriaContaEmImovelDeOutro() {
        assertNotFound(() -> post("/imoveis/" + imovelAId + "/contas", ownerBBearer, Map.of(
            "tipoContaId", UUID.randomUUID().toString(),
            "vencimento", LocalDate.of(2026, 11, 1).toString(),
            "valor", new BigDecimal("50.00"),
            "responsavel", "PROPRIETARIO")));
    }

    @Test
    void proprietarioAlheioNaoAtualizaChamadoDeOutro() {
        assertNotFound(() -> patch("/chamados/" + chamadoAId, ownerBBearer, Map.of("status", "EM_ANDAMENTO")));
    }

    @Test
    void inquilinoSemContratoNaoAbreChamadoEmImovelAlheio() {
        assertNotFound(() -> post("/imoveis/" + imovelAId + "/chamados", tenantCBearer,
            Map.of("categoriaId", UUID.randomUUID().toString(), "descricao", "tentativa")));
    }

    @Test
    void inquilinoSemContratoNaoVeCategoriasDeChamadoDeImovelAlheio() {
        assertNotFound(() -> getList("/imoveis/" + imovelAId + "/categorias-chamado", tenantCBearer));
    }

    @Test
    void inquilinoAlheioNaoVeContratoDeOutroInquilino() {
        assertNotFound(() -> getJson("/contratos/" + contratoAId, tenantCBearer));
    }

    private void assertNotFound(Runnable action) {
        var ex = assertThrows(HttpClientResponseException.class, action::run);
        assertThat(ex.getStatus().getCode()).isEqualTo(HttpStatus.NOT_FOUND.getCode());
    }

    private String login(String email, String nome, String cpfCnpj) {
        Map<?, ?> convite = client.toBlocking().retrieve(HttpRequest.POST("/auth/convites/proprietarios",
            Map.of("email", email, "nome", nome, "cpfCnpj", cpfCnpj)), Map.class);
        return loginComSenha(email, "Senha1234", convite.get("token"));
    }

    private String loginComSenha(String email, String senha, Object conviteToken) {
        Map<?, ?> aceite = client.toBlocking().retrieve(HttpRequest.POST(
            "/auth/convites/" + conviteToken + "/aceitar",
            Map.of("email", email, "senha", senha)), Map.class);
        return (String) aceite.get("sessionToken");
    }

    private String loginComSenha(String email, String senha) {
        Map<?, ?> aceite = client.toBlocking().retrieve(HttpRequest.POST("/auth/login",
            Map.of("email", email, "senha", senha)), Map.class);
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> patch(String path, String bearer, Object body) {
        MutableHttpRequest<Object> req = HttpRequest.PATCH(path, body);
        if (bearer != null) req.header("Authorization", bearer);
        return client.toBlocking().retrieve(req, Map.class);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getJson(String path, String bearer) {
        MutableHttpRequest<Object> req = HttpRequest.GET(path);
        if (bearer != null) req.header("Authorization", bearer);
        return client.toBlocking().retrieve(req, Map.class);
    }

    private List<Map<String, Object>> getList(String path, String bearer) {
        MutableHttpRequest<Object> req = HttpRequest.GET(path);
        if (bearer != null) req.header("Authorization", bearer);
        return client.toBlocking().retrieve(req, Argument.listOf(Argument.mapOf(String.class, Object.class)));
    }
}
