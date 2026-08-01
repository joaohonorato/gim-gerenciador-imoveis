package br.com.imoveis.domain.arquivo;

public enum TipoArquivo {
    AVATAR_PROPRIETARIO("avatares"),
    AVATAR_INQUILINO("avatares"),
    FOTO_IMOVEL("fotos-imoveis"),
    DOCUMENTO_CONTRATO("documentos"),
    DOCUMENTO_GARANTIA("documentos");

    private final String container;

    TipoArquivo(String container) {
        this.container = container;
    }

    public String container() {
        return container;
    }

    public boolean privado() {
        return container.equals("documentos");
    }
}
