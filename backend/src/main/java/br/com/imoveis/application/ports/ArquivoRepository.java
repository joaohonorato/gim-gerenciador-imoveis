package br.com.imoveis.application.ports;

import br.com.imoveis.domain.arquivo.Arquivo;
import br.com.imoveis.domain.arquivo.TipoArquivo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArquivoRepository {
    Arquivo save(Arquivo arquivo);
    Optional<Arquivo> findById(UUID id);
    List<Arquivo> findByDonoIdAndTipo(UUID donoId, TipoArquivo tipo);
    void excluir(UUID id);
}
