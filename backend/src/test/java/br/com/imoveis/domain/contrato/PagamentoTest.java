package br.com.imoveis.domain.contrato;

import br.com.imoveis.domain.shared.Dinheiro;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PagamentoTest {

    @Test
    void confirmacaoTornaPago() {
        Pagamento p = Pagamento.pendente(UUID.randomUUID(), LocalDate.of(2026, 3, 10), Dinheiro.of(1000));
        p.confirmar(LocalDate.of(2026, 3, 8));
        assertThat(p.status()).isEqualTo(PagamentoStatus.PAGO);
        assertThat(p.pagoEm()).isEqualTo(LocalDate.of(2026, 3, 8));
    }

    @Test
    void confirmacaoDuplicadaFalha() {
        Pagamento p = Pagamento.pendente(UUID.randomUUID(), LocalDate.of(2026, 3, 10), Dinheiro.of(1000));
        p.confirmar(LocalDate.of(2026, 3, 8));
        assertThatThrownBy(() -> p.confirmar(LocalDate.of(2026, 3, 9)))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void marcaAtrasadoQuandoHojePassouDoVencimento() {
        Pagamento p = Pagamento.pendente(UUID.randomUUID(), LocalDate.of(2026, 3, 10), Dinheiro.of(1000));
        p.marcarAtrasadoSe(LocalDate.of(2026, 3, 12));
        assertThat(p.status()).isEqualTo(PagamentoStatus.ATRASADO);
    }

    @Test
    void naoMarcaAtrasadoAntesDoVencimento() {
        Pagamento p = Pagamento.pendente(UUID.randomUUID(), LocalDate.of(2026, 3, 10), Dinheiro.of(1000));
        p.marcarAtrasadoSe(LocalDate.of(2026, 3, 9));
        assertThat(p.status()).isEqualTo(PagamentoStatus.PENDENTE);
    }
}
