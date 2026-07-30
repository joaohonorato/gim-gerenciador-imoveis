package br.com.imoveis.domain.imovel;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UnidadeTest {

    @Test
    void reservaAPartirDeVaga() {
        Unidade u = Unidade.novaPadrao(UUID.randomUUID());
        u.reservar();
        assertThat(u.status()).isEqualTo(UnidadeStatus.RESERVADO);
    }

    @Test
    void naoReservaAlugada() {
        Unidade u = Unidade.novaPadrao(UUID.randomUUID());
        u.reservar();
        u.alugar();
        assertThatThrownBy(u::reservar).isInstanceOf(TransicaoInvalidaException.class);
    }

    @Test
    void aluguelExigeReserva() {
        Unidade u = Unidade.novaPadrao(UUID.randomUUID());
        assertThatThrownBy(u::alugar).isInstanceOf(TransicaoInvalidaException.class);
    }

    @Test
    void liberaVoltaParaVago() {
        Unidade u = Unidade.novaPadrao(UUID.randomUUID());
        u.reservar();
        u.alugar();
        u.liberar();
        assertThat(u.status()).isEqualTo(UnidadeStatus.VAGO);
    }
}
