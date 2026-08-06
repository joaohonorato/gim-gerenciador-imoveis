package br.com.imoveis.application.ports;

import br.com.imoveis.domain.imovel.TipoConta;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TipoContaRepository {
    TipoConta save(TipoConta tipoConta);
    Optional<TipoConta> findById(UUID id);
    List<TipoConta> findByProprietarioId(UUID proprietarioId);
    Optional<TipoConta> findByProprietarioIdAndNome(UUID proprietarioId, String nome);
}
