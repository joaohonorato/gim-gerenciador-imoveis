package br.com.imoveis.domain.contrato;

import br.com.imoveis.domain.shared.Dinheiro;
import br.com.imoveis.domain.shared.Periodo;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContratoTest {

    private Contrato novoContrato() {
        return Contrato.novo(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            new Periodo(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)),
            TipoContrato.RESIDENCIAL, Dinheiro.of(2500), "IPCA");
    }

    @Test
    void contratoNovoNasceComoPendente() {
        Contrato c = novoContrato();
        assertThat(c.statusAssinatura()).isEqualTo(ContratoAssinaturaStatus.PENDENTE);
        assertThat(c.pagamentos()).isEmpty();
    }

    @Test
    void garantiaNaoPodeSerSubstituida() {
        Contrato c = novoContrato();
        c.definirGarantia(GarantiaTipo.CAUCAO, LocalDate.of(2027, 1, 1), "{}");
        assertThatThrownBy(() ->
            c.definirGarantia(GarantiaTipo.FIADOR, LocalDate.of(2027, 1, 1), "{}"))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void umaAssinaturaNaoConcluiContrato() {
        Contrato c = novoContrato();
        c.assinar(ParteContrato.PROPRIETARIO);
        assertThat(c.statusAssinatura()).isEqualTo(ContratoAssinaturaStatus.PENDENTE);
        assertThat(c.pagamentos()).isEmpty();
    }

    @Test
    void assinaturaDeAmbasConcluiEGeraPagamentos() {
        Contrato c = novoContrato();
        c.assinar(ParteContrato.PROPRIETARIO);
        c.assinar(ParteContrato.INQUILINO);

        assertThat(c.statusAssinatura()).isEqualTo(ContratoAssinaturaStatus.ASSINADO);
        assertThat(c.pagamentos()).hasSize(c.periodo().meses());
        assertThat(c.pagamentos())
            .allMatch(p -> p.status() == PagamentoStatus.PENDENTE)
            .allMatch(p -> p.valor().valor().compareTo(Dinheiro.of(2500).valor()) == 0);
    }

    @Test
    void naoPermiteReassinar() {
        Contrato c = novoContrato();
        c.assinar(ParteContrato.PROPRIETARIO);
        c.assinar(ParteContrato.INQUILINO);
        assertThatThrownBy(() -> c.assinar(ParteContrato.PROPRIETARIO))
            .isInstanceOf(IllegalStateException.class);
    }
}
