package br.com.imoveis.application.ports;

import br.com.imoveis.domain.chamado.CategoriaChamado;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoriaChamadoRepository {
    CategoriaChamado save(CategoriaChamado categoriaChamado);
    Optional<CategoriaChamado> findById(UUID id);
    List<CategoriaChamado> findByProprietarioId(UUID proprietarioId);
    Optional<CategoriaChamado> findByProprietarioIdAndNome(UUID proprietarioId, String nome);
}
