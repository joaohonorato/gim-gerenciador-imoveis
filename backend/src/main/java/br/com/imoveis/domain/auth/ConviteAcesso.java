package br.com.imoveis.domain.auth;

import br.com.imoveis.domain.proprietario.PerfilProprietario;
import br.com.imoveis.domain.shared.Email;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

public class ConviteAcesso {

    private static final SecureRandom RNG = new SecureRandom();

    private final UUID id;
    private final String token;
    private final Email email;
    private final TipoContaAcesso tipo;
    private final String nome;
    private final String documento;
    private final PerfilProprietario perfilProprietario;
    private final Instant expiraEm;
    private boolean consumido;

    private ConviteAcesso(UUID id, String token, Email email, TipoContaAcesso tipo, String nome,
                          String documento, PerfilProprietario perfilProprietario,
                          Instant expiraEm, boolean consumido) {
        this.id = id;
        this.token = Objects.requireNonNull(token, "token obrigatório");
        this.email = Objects.requireNonNull(email, "email obrigatório");
        this.tipo = Objects.requireNonNull(tipo, "tipo obrigatório");
        this.nome = validaTexto(nome, "nome obrigatório");
        this.documento = validaTexto(documento, "documento obrigatório");
        this.perfilProprietario = perfilProprietario;
        this.expiraEm = Objects.requireNonNull(expiraEm, "expiraEm obrigatório");
        this.consumido = consumido;
    }

    public static ConviteAcesso convidarProprietario(Email email, String nome, String cpfCnpj,
                                                     PerfilProprietario perfil, Instant agora) {
        return new ConviteAcesso(UUID.randomUUID(), gerarToken(), email, TipoContaAcesso.PROPRIETARIO,
            nome, cpfCnpj, perfil == null ? PerfilProprietario.OWNER : perfil,
            agora.plus(7, ChronoUnit.DAYS), false);
    }

    public static ConviteAcesso reconstituir(UUID id, String token, Email email, TipoContaAcesso tipo,
                                             String nome, String documento,
                                             PerfilProprietario perfilProprietario,
                                             Instant expiraEm, boolean consumido) {
        return new ConviteAcesso(id, token, email, tipo, nome, documento, perfilProprietario, expiraEm, consumido);
    }

    public boolean expirado(Instant agora) {
        return agora.isAfter(expiraEm);
    }

    public void consumir() {
        if (consumido) {
            throw new IllegalStateException("convite já consumido");
        }
        consumido = true;
    }

    private static String validaTexto(String valor, String erro) {
        Objects.requireNonNull(valor, erro);
        if (valor.isBlank()) {
            throw new IllegalArgumentException(erro);
        }
        return valor.trim();
    }

    private static String gerarToken() {
        byte[] bytes = new byte[24];
        RNG.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public UUID id() { return id; }
    public String token() { return token; }
    public Email email() { return email; }
    public TipoContaAcesso tipo() { return tipo; }
    public String nome() { return nome; }
    public String documento() { return documento; }
    public PerfilProprietario perfilProprietario() { return perfilProprietario; }
    public Instant expiraEm() { return expiraEm; }
    public boolean consumido() { return consumido; }
}