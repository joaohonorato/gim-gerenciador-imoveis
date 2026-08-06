package br.com.imoveis.domain.convite;

import br.com.imoveis.domain.contrato.GarantiaTipo;
import br.com.imoveis.domain.contrato.TipoContrato;
import br.com.imoveis.domain.shared.Dinheiro;
import br.com.imoveis.domain.shared.Periodo;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConviteTest {

    private Convite novoConvite() {
        Convite.CondicoesConvite condicoes = new Convite.CondicoesConvite(
            TipoContrato.RESIDENCIAL, Dinheiro.of(1500),
            new Periodo(LocalDate.of(2026, 9, 1), LocalDate.of(2027, 9, 1)), GarantiaTipo.CAUCAO);
        return Convite.gerar(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), condicoes, Instant.now());
    }

    @Test
    void expiradoEVerdadeiroDepoisDoPrazo() {
        Convite c = novoConvite();
        Instant depoisDoPrazo = c.expiraEm().plus(1, ChronoUnit.SECONDS);
        assertThat(c.expirado(depoisDoPrazo)).isTrue();
        assertThat(c.expirado(c.expiraEm().minus(1, ChronoUnit.SECONDS))).isFalse();
    }

    @Test
    void renovarEstendePrazoSemTrocarToken() {
        Convite c = novoConvite();
        String tokenOriginal = c.token();
        Instant expiraEmOriginal = c.expiraEm();
        Instant expirado = expiraEmOriginal.plus(1, ChronoUnit.DAYS);

        assertThat(c.expirado(expirado)).isTrue();

        c.renovar(expirado);

        assertThat(c.token()).isEqualTo(tokenOriginal);
        assertThat(c.expiraEm()).isAfter(expiraEmOriginal);
        assertThat(c.expirado(expirado)).isFalse();
    }

    @Test
    void renovarSoFuncionaEnquantoPendente() {
        Convite c = novoConvite();
        c.revogar();

        assertThatThrownBy(() -> c.renovar(Instant.now()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("não pode ser renovado");
    }

    @Test
    void ativoEFalsoParaRevogadoERecusadoIndependenteDoPrazo() {
        Convite revogado = novoConvite();
        revogado.revogar();
        assertThat(revogado.ativo(Instant.now())).isFalse();
    }
}
