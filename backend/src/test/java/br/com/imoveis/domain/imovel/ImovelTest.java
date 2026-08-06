package br.com.imoveis.domain.imovel;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImovelTest {

    @Test
    void cadastroCriaUnidadePadraoAutomatica() {
        Imovel imovel = Imovel.cadastrar(UUID.randomUUID(), "Rua A, 100", "SP", "M-1", Instant.now());
        assertThat(imovel.unidades()).hasSize(1);
        assertThat(imovel.unidadePadrao().padrao()).isTrue();
        assertThat(imovel.unidadePadrao().status()).isEqualTo(UnidadeStatus.VAGO);
    }

    @Test
    void novoImovelEhPrivadoPorPadrao() {
        Imovel imovel = Imovel.cadastrar(UUID.randomUUID(), "Rua A, 100", "SP", "M-1", Instant.now());
        assertThat(imovel.visibilidade()).isEqualTo(Visibilidade.PRIVADO);
    }

    @Test
    void enderecoObrigatorio() {
        assertThatThrownBy(() -> Imovel.cadastrar(UUID.randomUUID(), " ", "SP", "M-1", Instant.now()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void adicionaUnidadesEmLote() {
        Imovel imovel = Imovel.cadastrar(UUID.randomUUID(), "Rua A, 100", "SP", "M-1", Instant.now());
        List<Unidade> novas = imovel.adicionarUnidades(List.of("Casa 1 - Apto 1", "Casa 1 - Apto 2"));

        assertThat(novas).hasSize(2);
        assertThat(novas).allMatch(u -> !u.padrao());
        assertThat(novas).allMatch(u -> u.status() == UnidadeStatus.VAGO);
        // padrão automática + as 2 novas
        assertThat(imovel.unidades()).hasSize(3);
        assertThat(imovel.unidades()).extracting(Unidade::nome)
            .contains("Imóvel completo", "Casa 1 - Apto 1", "Casa 1 - Apto 2");
    }

    @Test
    void adicionarUnidadesExigePeloMenosUmNome() {
        Imovel imovel = Imovel.cadastrar(UUID.randomUUID(), "Rua A, 100", "SP", "M-1", Instant.now());
        assertThatThrownBy(() -> imovel.adicionarUnidades(List.of()))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
