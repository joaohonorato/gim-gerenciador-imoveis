package br.com.imoveis.domain.shared;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailTest {

    @Test
    void normalizaParaMinusculas() {
        Email e = new Email("Foo@Example.COM");
        assertThat(e.value()).isEqualTo("foo@example.com");
    }

    @Test
    void rejeitaSemArroba() {
        assertThatThrownBy(() -> new Email("naoehemail"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejeitaVazio() {
        assertThatThrownBy(() -> new Email(""))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
