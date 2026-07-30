package br.com.imoveis.domain.imovel;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImovelTest {

    @Test
    void cadastroCriaUnidadePadraoAutomatica() {
        Imovel imovel = Imovel.cadastrar(UUID.randomUUID(), "Rua A, 100", "SP", "M-1");
        assertThat(imovel.unidades()).hasSize(1);
        assertThat(imovel.unidadePadrao().padrao()).isTrue();
        assertThat(imovel.unidadePadrao().status()).isEqualTo(UnidadeStatus.VAGO);
    }

    @Test
    void novoImovelEhPrivadoPorPadrao() {
        Imovel imovel = Imovel.cadastrar(UUID.randomUUID(), "Rua A, 100", "SP", "M-1");
        assertThat(imovel.visibilidade()).isEqualTo(Visibilidade.PRIVADO);
    }

    @Test
    void enderecoObrigatorio() {
        assertThatThrownBy(() -> Imovel.cadastrar(UUID.randomUUID(), " ", "SP", "M-1"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
