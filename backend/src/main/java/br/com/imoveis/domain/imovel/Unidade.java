package br.com.imoveis.domain.imovel;

import java.util.Objects;
import java.util.UUID;

public class Unidade {

    private final UUID id;
    private final UUID imovelId;
    private String nome;
    private final boolean padrao;
    private UnidadeStatus status;

    Unidade(UUID id, UUID imovelId, String nome, boolean padrao, UnidadeStatus status) {
        this.id = id;
        this.imovelId = imovelId;
        this.nome = nome;
        this.padrao = padrao;
        this.status = status;
    }

    public static Unidade novaPadrao(UUID imovelId) {
        return new Unidade(UUID.randomUUID(), imovelId, "Imóvel completo", true, UnidadeStatus.VAGO);
    }

    // Unidade adicional além da padrão automática — ver
    // Imovel.adicionarUnidades. Usada quando o imóvel é subdividido (ex.:
    // terreno com várias casas, cada uma com vários apartamentos).
    public static Unidade nova(UUID imovelId, String nome) {
        Objects.requireNonNull(imovelId, "imovelId obrigatório");
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("nome obrigatório");
        }
        return new Unidade(UUID.randomUUID(), imovelId, nome.trim(), false, UnidadeStatus.VAGO);
    }

    public static Unidade reconstituir(UUID id, UUID imovelId, String nome, boolean padrao, UnidadeStatus status) {
        Objects.requireNonNull(id, "id obrigatório");
        Objects.requireNonNull(imovelId, "imovelId obrigatório");
        Objects.requireNonNull(status, "status obrigatório");
        return new Unidade(id, imovelId, nome, padrao, status);
    }

    public void reservar() {
        if (status != UnidadeStatus.VAGO) {
            throw new TransicaoInvalidaException("Só é possível reservar uma unidade VAGA (atual: " + status + ")");
        }
        status = UnidadeStatus.RESERVADO;
    }

    public void alugar() {
        if (status != UnidadeStatus.RESERVADO) {
            throw new TransicaoInvalidaException("Só é possível alugar unidade RESERVADA (atual: " + status + ")");
        }
        status = UnidadeStatus.ALUGADO;
    }

    public void liberar() {
        if (status == UnidadeStatus.VAGO) {
            throw new TransicaoInvalidaException("Unidade já está VAGA");
        }
        status = UnidadeStatus.VAGO;
    }

    public void entrarEmManutencao() {
        if (status == UnidadeStatus.ALUGADO) {
            throw new TransicaoInvalidaException("Unidade ALUGADA não pode entrar em manutenção sem liberar primeiro");
        }
        status = UnidadeStatus.MANUTENCAO;
    }

    public UUID id() { return id; }
    public UUID imovelId() { return imovelId; }
    public String nome() { return nome; }
    public boolean padrao() { return padrao; }
    public UnidadeStatus status() { return status; }
}
