package br.com.imoveis.domain.imovel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Imovel {

    private final UUID id;
    private final UUID proprietarioId;
    private String endereco;
    private String cidade;
    private String matricula;
    private String numero;
    private String bairro;
    private String complemento;
    private TipoImovel tipoImovel;
    private Visibilidade visibilidade;
    private final List<Unidade> unidades;

    private Imovel(UUID id, UUID proprietarioId, String endereco, String cidade,
                   String matricula, String numero, String bairro, String complemento,
                   TipoImovel tipoImovel, Visibilidade visibilidade, List<Unidade> unidades) {
        this.id = id;
        this.proprietarioId = proprietarioId;
        this.endereco = endereco;
        this.cidade = cidade;
        this.matricula = matricula;
        this.numero = numero;
        this.bairro = bairro;
        this.complemento = complemento;
        this.tipoImovel = tipoImovel;
        this.visibilidade = visibilidade;
        this.unidades = unidades;
    }

    public static Imovel cadastrar(UUID proprietarioId, String endereco, String cidade, String matricula) {
        return cadastrar(proprietarioId, endereco, cidade, matricula, null, null, null, null);
    }

    public static Imovel cadastrar(UUID proprietarioId, String endereco, String cidade, String matricula,
                                   String numero, String bairro, String complemento, TipoImovel tipoImovel) {
        Objects.requireNonNull(proprietarioId, "proprietarioId obrigatório");
        exigirNaoBranco(endereco, "endereco");
        exigirNaoBranco(cidade, "cidade");
        exigirNaoBranco(matricula, "matricula");
        UUID id = UUID.randomUUID();
        List<Unidade> unidades = new ArrayList<>();
        unidades.add(Unidade.novaPadrao(id));
        return new Imovel(id, proprietarioId, endereco.trim(), cidade.trim(),
            matricula.trim(), normalizarOpcional(numero), normalizarOpcional(bairro),
            normalizarOpcional(complemento), tipoImovel, Visibilidade.PRIVADO, unidades);
    }

    public static Imovel reconstituir(UUID id, UUID proprietarioId, String endereco, String cidade,
                                       String matricula, Visibilidade visibilidade, List<Unidade> unidades) {
        return reconstituir(id, proprietarioId, endereco, cidade, matricula, null, null, null, null, visibilidade, unidades);
    }

    public static Imovel reconstituir(UUID id, UUID proprietarioId, String endereco, String cidade,
                                      String matricula, String numero, String bairro, String complemento,
                                      TipoImovel tipoImovel, Visibilidade visibilidade, List<Unidade> unidades) {
        return new Imovel(id, proprietarioId, endereco, cidade, matricula, numero,
            bairro, complemento, tipoImovel, visibilidade, new ArrayList<>(unidades));
    }

    public Unidade unidadePadrao() {
        return unidades.stream()
            .filter(Unidade::padrao)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("imóvel sem unidade padrão: " + id));
    }

    private static void exigirNaoBranco(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " obrigatório");
        }
    }

    private static String normalizarOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }

    public String enderecoCompleto() {
        StringBuilder sb = new StringBuilder(endereco);
        if (numero != null) sb.append(", ").append(numero);
        if (complemento != null) sb.append(" - ").append(complemento);
        if (bairro != null) sb.append(" - ").append(bairro);
        sb.append(" - ").append(cidade);
        return sb.toString();
    }

    public UUID id() { return id; }
    public UUID proprietarioId() { return proprietarioId; }
    public String endereco() { return endereco; }
    public String cidade() { return cidade; }
    public String matricula() { return matricula; }
    public String numero() { return numero; }
    public String bairro() { return bairro; }
    public String complemento() { return complemento; }
    public TipoImovel tipoImovel() { return tipoImovel; }
    public Visibilidade visibilidade() { return visibilidade; }
    public List<Unidade> unidades() { return Collections.unmodifiableList(unidades); }
}
