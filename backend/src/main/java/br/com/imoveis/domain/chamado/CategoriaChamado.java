package br.com.imoveis.domain.chamado;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

// Catálogo de categorias de chamado por proprietário — antes um enum fixo
// (ELETRICA/HIDRAULICA/ESTRUTURAL/OUTRO). Cada proprietário tem seu próprio
// catálogo (mesmo padrão de TipoConta), com um conjunto padrão semeado na
// criação do proprietário via SemearCatalogosPadrao — ver esse use case.
public class CategoriaChamado {

    private final UUID id;
    private final UUID proprietarioId;
    private final String nome;
    private final Instant criadoEm;

    private CategoriaChamado(UUID id, UUID proprietarioId, String nome, Instant criadoEm) {
        this.id = id;
        this.proprietarioId = proprietarioId;
        this.nome = nome;
        this.criadoEm = criadoEm;
    }

    public static CategoriaChamado registrar(UUID proprietarioId, String nome, Instant agora) {
        Objects.requireNonNull(proprietarioId, "proprietarioId obrigatório");
        Objects.requireNonNull(nome, "nome obrigatório");
        if (nome.isBlank()) throw new IllegalArgumentException("nome obrigatório");
        return new CategoriaChamado(UUID.randomUUID(), proprietarioId, nome.trim(), agora);
    }

    public static CategoriaChamado reconstituir(UUID id, UUID proprietarioId, String nome, Instant criadoEm) {
        return new CategoriaChamado(id, proprietarioId, nome, criadoEm);
    }

    public UUID id() { return id; }
    public UUID proprietarioId() { return proprietarioId; }
    public String nome() { return nome; }
    public Instant criadoEm() { return criadoEm; }
}
