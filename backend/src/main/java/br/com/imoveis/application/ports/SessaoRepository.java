package br.com.imoveis.application.ports;

import br.com.imoveis.domain.auth.TipoContaAcesso;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface SessaoRepository {

    record Sessao(String token, UUID contaAcessoId, TipoContaAcesso tipoConta, UUID proprietarioId,
                  UUID inquilinoId, Instant expiraEm) {}

    Sessao save(Sessao sessao);
    Optional<Sessao> findByToken(String token);
    void deleteByToken(String token);
}
