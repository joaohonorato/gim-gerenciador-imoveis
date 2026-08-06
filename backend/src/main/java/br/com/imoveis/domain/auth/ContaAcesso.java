package br.com.imoveis.domain.auth;

import br.com.imoveis.domain.shared.Email;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class ContaAcesso {

    private final UUID id;
    private Email email;
    private String senhaHash;
    private final TipoContaAcesso tipo;
    private final UUID proprietarioId;
    private final UUID inquilinoId;
    private final Instant criadoEm;
    private boolean emailVerificado;
    private Email emailPendente;

    private ContaAcesso(UUID id, Email email, String senhaHash, TipoContaAcesso tipo,
                        UUID proprietarioId, UUID inquilinoId, Instant criadoEm, boolean emailVerificado,
                        Email emailPendente) {
        this.id = id;
        this.email = Objects.requireNonNull(email, "email obrigatório");
        this.senhaHash = validaSenhaHash(senhaHash);
        this.tipo = Objects.requireNonNull(tipo, "tipo obrigatório");
        this.proprietarioId = proprietarioId;
        this.inquilinoId = inquilinoId;
        this.criadoEm = Objects.requireNonNull(criadoEm, "criadoEm obrigatório");
        this.emailVerificado = emailVerificado;
        this.emailPendente = emailPendente;
    }

    public static ContaAcesso vincularProprietario(Email email, String senhaHash, UUID proprietarioId, Instant agora) {
        Objects.requireNonNull(proprietarioId, "proprietarioId obrigatório");
        return new ContaAcesso(UUID.randomUUID(), email, senhaHash, TipoContaAcesso.PROPRIETARIO,
            proprietarioId, null, agora, false, null);
    }

    public static ContaAcesso vincularInquilino(Email email, String senhaHash, UUID inquilinoId, Instant agora) {
        Objects.requireNonNull(inquilinoId, "inquilinoId obrigatório");
        return new ContaAcesso(UUID.randomUUID(), email, senhaHash, TipoContaAcesso.INQUILINO,
            null, inquilinoId, agora, false, null);
    }

    public static ContaAcesso reconstituir(UUID id, Email email, String senhaHash, TipoContaAcesso tipo,
                                           UUID proprietarioId, UUID inquilinoId, Instant criadoEm,
                                           boolean emailVerificado, Email emailPendente) {
        return new ContaAcesso(id, email, senhaHash, tipo, proprietarioId, inquilinoId, criadoEm,
            emailVerificado, emailPendente);
    }

    public void redefinirSenha(String novoHash) {
        this.senhaHash = validaSenhaHash(novoHash);
    }

    public void verificarEmail() {
        if (emailVerificado) {
            throw new IllegalStateException("e-mail já verificado");
        }
        emailVerificado = true;
    }

    // Não aplica na hora — fica pendente até confirmarAlteracaoEmail() ser
    // chamado a partir do token enviado pro *novo* endereço. Assim um erro
    // de digitação no novo e-mail nunca troca o e-mail de login sem prova de
    // que o usuário realmente tem acesso a ele.
    public void solicitarAlteracaoEmail(Email novoEmail) {
        Objects.requireNonNull(novoEmail, "novoEmail obrigatório");
        if (novoEmail.equals(this.email)) {
            throw new IllegalArgumentException("novo e-mail é igual ao atual");
        }
        this.emailPendente = novoEmail;
    }

    public void confirmarAlteracaoEmail() {
        if (emailPendente == null) {
            throw new IllegalStateException("não há alteração de e-mail pendente");
        }
        this.email = emailPendente;
        this.emailPendente = null;
        // Clicar no link de confirmação enviado pro novo endereço já prova
        // posse dele — não faz sentido pedir uma segunda verificação.
        this.emailVerificado = true;
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
    public boolean emailVerificado() { return emailVerificado; }
    public Email emailPendente() { return emailPendente; }
}
