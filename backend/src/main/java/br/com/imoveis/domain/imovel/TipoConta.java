package br.com.imoveis.domain.imovel;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

// Catálogo de tipos de conta por proprietário — antes um enum fixo
// (IPTU/CONDOMINIO/AGUA/LUZ). Cada proprietário cadastra os próprios tipos,
// reutilizáveis em qualquer imóvel dele (não preso a um imóvel específico).
public class TipoConta {

    private final UUID id;
    private final UUID proprietarioId;
    private final String nome;
    private final Instant criadoEm;

    private TipoConta(UUID id, UUID proprietarioId, String nome, Instant criadoEm) {
        this.id = id;
        this.proprietarioId = proprietarioId;
        this.nome = nome;
        this.criadoEm = criadoEm;
    }

    public static TipoConta registrar(UUID proprietarioId, String nome, Instant agora) {
        Objects.requireNonNull(proprietarioId, "proprietarioId obrigatório");
        Objects.requireNonNull(nome, "nome obrigatório");
        if (nome.isBlank()) throw new IllegalArgumentException("nome obrigatório");
        return new TipoConta(UUID.randomUUID(), proprietarioId, nome.trim(), agora);
    }

    public static TipoConta reconstituir(UUID id, UUID proprietarioId, String nome, Instant criadoEm) {
        return new TipoConta(id, proprietarioId, nome, criadoEm);
    }

    public UUID id() { return id; }
    public UUID proprietarioId() { return proprietarioId; }
    public String nome() { return nome; }
    public Instant criadoEm() { return criadoEm; }
}
