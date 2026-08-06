package br.com.imoveis.domain.auth;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class PushToken {

    private final UUID id;
    private final UUID contaAcessoId;
    private final String token;
    private final Instant criadoEm;

    private PushToken(UUID id, UUID contaAcessoId, String token, Instant criadoEm) {
        this.id = id;
        this.contaAcessoId = Objects.requireNonNull(contaAcessoId, "contaAcessoId obrigatório");
        this.token = validarToken(token);
        this.criadoEm = Objects.requireNonNull(criadoEm, "criadoEm obrigatório");
    }

    public static PushToken registrar(UUID contaAcessoId, String token, Instant agora) {
        return new PushToken(UUID.randomUUID(), contaAcessoId, token, agora);
    }

    public static PushToken reconstituir(UUID id, UUID contaAcessoId, String token, Instant criadoEm) {
        return new PushToken(id, contaAcessoId, token, criadoEm);
    }

    private static String validarToken(String token) {
        Objects.requireNonNull(token, "token obrigatório");
        if (token.isBlank()) {
            throw new IllegalArgumentException("token obrigatório");
        }
        return token;
    }

    public UUID id() { return id; }
    public UUID contaAcessoId() { return contaAcessoId; }
    public String token() { return token; }
    public Instant criadoEm() { return criadoEm; }
}
