package br.com.imoveis.domain.shared;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PeriodoTest {

    @Test
    void fimAntesDoInicioLanca() {
        assertThatThrownBy(() -> new Periodo(LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 5)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mesmoDiaEValido() {
        Periodo p = new Periodo(LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 10));
        assertThat(p.meses()).isEqualTo(1);
    }

    @Test
    void periodosDisjuntosNaoSobrepoem() {
        Periodo a = new Periodo(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));
        Periodo b = new Periodo(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 31));
        assertThat(a.overlaps(b)).isFalse();
        assertThat(b.overlaps(a)).isFalse();
    }

    @Test
    void periodosContiguousNaBordaSobrepoem() {
        Periodo a = new Periodo(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));
        Periodo b = new Periodo(LocalDate.of(2026, 6, 30), LocalDate.of(2026, 12, 31));
        assertThat(a.overlaps(b)).isTrue();
    }

    @Test
    void periodoContidoSobrepoe() {
        Periodo grande = new Periodo(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        Periodo pequeno = new Periodo(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 30));
        assertThat(grande.overlaps(pequeno)).isTrue();
        assertThat(pequeno.overlaps(grande)).isTrue();
    }

    @Test
    void mesesCalculadosParaAnoCompleto() {
        Periodo p = new Periodo(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        assertThat(p.meses()).isEqualTo(11);
    }
}
