package br.com.imoveis.application.ports;

import br.com.imoveis.domain.chamado.Chamado;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChamadoRepository {
    Chamado save(Chamado chamado);
    Optional<Chamado> findById(UUID id);
    List<Chamado> findByImovel(UUID imovelId);
    List<Chamado> findByImovelIds(List<UUID> imovelIds);
    List<Chamado> findByInquilino(UUID inquilinoId);
}
