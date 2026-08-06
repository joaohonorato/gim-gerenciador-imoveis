package br.com.imoveis.domain.auth;

import br.com.imoveis.domain.shared.Email;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

/**
 * Token de curta duração emitido por e-mail para uma conta de acesso já
 * existente — reset de senha ou confirmação de e-mail. Mesma forma de
 * {@link ConviteAcesso} (token+expiração+consumo), discriminado por
 * {@link TokenContaFinalidade} em vez de uma tabela por finalidade.
 */
public class TokenConta {

    private static final SecureRandom RNG = new SecureRandom();

    private final UUID id;
    private final String token;
    private final Email email;
    private final TokenContaFinalidade finalidade;
    private final Instant expiraEm;
    private final Instant criadoEm;
    private boolean consumido;

    private TokenConta(UUID id, String token, Email email, TokenContaFinalidade finalidade,
                       Instant expiraEm, Instant criadoEm, boolean consumido) {
        this.id = id;
        this.token = Objects.requireNonNull(token, "token obrigatório");
        this.email = Objects.requireNonNull(email, "email obrigatório");
        this.finalidade = Objects.requireNonNull(finalidade, "finalidade obrigatória");
        this.expiraEm = Objects.requireNonNull(expiraEm, "expiraEm obrigatório");
        this.criadoEm = Objects.requireNonNull(criadoEm, "criadoEm obrigatório");
        this.consumido = consumido;
    }

    public static TokenConta paraResetSenha(Email email, Instant agora) {
        return new TokenConta(UUID.randomUUID(), gerarToken(), email, TokenContaFinalidade.RESET_SENHA,
            agora.plus(1, ChronoUnit.HOURS), agora, false);
    }

    public static TokenConta paraVerificacaoEmail(Email email, Instant agora) {
        return new TokenConta(UUID.randomUUID(), gerarToken(), email, TokenContaFinalidade.VERIFICACAO_EMAIL,
            agora.plus(24, ChronoUnit.HOURS), agora, false);
    }

    // `email` aqui é o *novo* endereço solicitado (ainda em
    // ContaAcesso.emailPendente, não em ContaAcesso.email) — é por esse
    // valor que ConfirmarVerificacaoEmail localiza a conta na confirmação.
    public static TokenConta paraAlteracaoEmail(Email novoEmail, Instant agora) {
        return new TokenConta(UUID.randomUUID(), gerarToken(), novoEmail, TokenContaFinalidade.ALTERACAO_EMAIL,
            agora.plus(24, ChronoUnit.HOURS), agora, false);
    }

    public static TokenConta reconstituir(UUID id, String token, Email email, TokenContaFinalidade finalidade,
                                          Instant expiraEm, Instant criadoEm, boolean consumido) {
        return new TokenConta(id, token, email, finalidade, expiraEm, criadoEm, consumido);
    }

    public boolean expirado(Instant agora) {
        return agora.isAfter(expiraEm);
    }

    public void consumir() {
        if (consumido) {
            throw new IllegalStateException("token já consumido");
        }
        consumido = true;
    }

    private static String gerarToken() {
        byte[] bytes = new byte[24];
        RNG.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public UUID id() { return id; }
    public String token() { return token; }
    public Email email() { return email; }
    public TokenContaFinalidade finalidade() { return finalidade; }
    public Instant expiraEm() { return expiraEm; }
    public Instant criadoEm() { return criadoEm; }
    public boolean consumido() { return consumido; }
}
