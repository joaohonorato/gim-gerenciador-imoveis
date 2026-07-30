package br.com.imoveis.domain.shared;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CpfTest {

    @Test
    void aceitaCpfValidoComFormatacao() {
        Cpf cpf = new Cpf("529.982.247-25");
        assertThat(cpf.value()).isEqualTo("52998224725");
    }

    @Test
    void aceitaCpfValidoSemFormatacao() {
        Cpf cpf = new Cpf("52998224725");
        assertThat(cpf.value()).isEqualTo("52998224725");
    }

    @Test
    void rejeitaCpfComDigitoErrado() {
        assertThatThrownBy(() -> new Cpf("529.982.247-26"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejeitaCpfComTodosDigitosIguais() {
        assertThatThrownBy(() -> new Cpf("11111111111"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejeitaCpfComQuantidadeErrada() {
        assertThatThrownBy(() -> new Cpf("123"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseSelecionaCpfPelaQuantidade() {
        CpfCnpj doc = CpfCnpj.parse("529.982.247-25");
        assertThat(doc).isInstanceOf(Cpf.class);
        assertThat(doc.digits()).isEqualTo("52998224725");
    }

    @Test
    void parseSelecionaCnpjPelaQuantidade() {
        CpfCnpj doc = CpfCnpj.parse("11.222.333/0001-81");
        assertThat(doc).isInstanceOf(Cnpj.class);
        assertThat(doc.digits()).isEqualTo("11222333000181");
    }
}
