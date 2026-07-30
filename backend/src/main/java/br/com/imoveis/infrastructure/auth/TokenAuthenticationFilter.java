package br.com.imoveis.infrastructure.auth;

import br.com.imoveis.application.ports.Clock;
import br.com.imoveis.application.ports.SessaoRepository;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import org.reactivestreams.Publisher;

import java.util.Optional;

@Filter("/**")
public class TokenAuthenticationFilter implements HttpServerFilter {

    public static final String PRINCIPAL_ATTR = "principal";

    private final SessaoRepository sessaoRepository;
    private final Clock clock;

    public TokenAuthenticationFilter(SessaoRepository sessaoRepository, Clock clock) {
        this.sessaoRepository = sessaoRepository;
        this.clock = clock;
    }

    @Override
    @ExecuteOn(TaskExecutors.BLOCKING)
    public Publisher<MutableHttpResponse<?>> doFilter(@NonNull HttpRequest<?> request, @NonNull ServerFilterChain chain) {
        extractToken(request)
            .flatMap(sessaoRepository::findByToken)
            .filter(s -> clock.now().isBefore(s.expiraEm()))
            .ifPresent(s -> request.setAttribute(PRINCIPAL_ATTR,
                new Principal(s.contaAcessoId(), s.tipoConta(), s.proprietarioId(), s.inquilinoId())));
        return chain.proceed(request);
    }

    private Optional<String> extractToken(HttpRequest<?> request) {
        return request.getHeaders().getAuthorization()
            .filter(h -> h.startsWith("Bearer "))
            .map(h -> h.substring("Bearer ".length()).trim());
    }
}
