package br.com.imoveis.domain.auth;

import br.com.imoveis.domain.shared.Email;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class ContaAcesso {

    private final UUID id;
    private final Email email;
    private final String senhaHash;
    private final TipoContaAcesso tipo;
    private final UUID proprietarioId;
    private final UUID inquilinoId;
    private final Instant criadoEm;

    private ContaAcesso(UUID id, Email email, String senhaHash, TipoContaAcesso tipo,
                        UUID proprietarioId, UUID inquilinoId, Instant criadoEm) {
        this.id = id;
        this.email = Objects.requireNonNull(email, "email obrigatório");
        this.senhaHash = validaSenhaHash(senhaHash);
        this.tipo = Objects.requireNonNull(tipo, "tipo obrigatório");
        this.proprietarioId = proprietarioId;
        this.inquilinoId = inquilinoId;
        this.criadoEm = Objects.requireNonNull(criadoEm, "criadoEm obrigatório");
    }

    public static ContaAcesso vincularProprietario(Email email, String senhaHash, UUID proprietarioId, Instant agora) {
        Objects.requireNonNull(proprietarioId, "proprietarioId obrigatório");
        return new ContaAcesso(UUID.randomUUID(), email, senhaHash, TipoContaAcesso.PROPRIETARIO,
            proprietarioId, null, agora);
    }

    public static ContaAcesso vincularInquilino(Email email, String senhaHash, UUID inquilinoId, Instant agora) {
        Objects.requireNonNull(inquilinoId, "inquilinoId obrigatório");
        return new ContaAcesso(UUID.randomUUID(), email, senhaHash, TipoContaAcesso.INQUILINO,
            null, inquilinoId, agora);
    }

    public static ContaAcesso reconstituir(UUID id, Email email, String senhaHash, TipoContaAcesso tipo,
                                           UUID proprietarioId, UUID inquilinoId, Instant criadoEm) {
        return new ContaAcesso(id, email, senhaHash, tipo, proprietarioId, inquilinoId, criadoEm);
    }

    private static String validaSenhaHash(String senhaHash) {
        Objects.requireNonNull(senhaHash, "senhaHash obrigatória");
        if (senhaHash.isBlank()) {
            throw new IllegalArgumentException("senhaHash obrigatória");
        }
        return senhaHash;
    }

    public UUID id() { return id; }
    public Email email() { return email; }
    public String senhaHash() { return senhaHash; }
    public TipoContaAcesso tipo() { return tipo; }
    public UUID proprietarioId() { return proprietarioId; }
    public UUID inquilinoId() { return inquilinoId; }
    public Instant criadoEm() { return criadoEm; }
}