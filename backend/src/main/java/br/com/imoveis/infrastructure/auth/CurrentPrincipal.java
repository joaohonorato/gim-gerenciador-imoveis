package br.com.imoveis.infrastructure.auth;

import br.com.imoveis.application.exception.AutenticacaoInvalidaException;
import io.micronaut.http.HttpRequest;

public final class CurrentPrincipal {
    private CurrentPrincipal() {}

    public static Principal require(HttpRequest<?> request) {
        return request.<Principal>getAttribute(TokenAuthenticationFilter.PRINCIPAL_ATTR, Principal.class)
            .orElseThrow(() -> new AutenticacaoInvalidaException("autenticação obrigatória"));
    }
}
