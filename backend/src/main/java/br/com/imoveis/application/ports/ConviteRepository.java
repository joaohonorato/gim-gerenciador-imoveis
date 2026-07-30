package br.com.imoveis.application.ports;

import br.com.imoveis.domain.convite.Candidatura;
import br.com.imoveis.domain.convite.Convite;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConviteRepository {
    Convite save(Convite convite);
    Optional<Convite> findByToken(String token);
    Optional<Convite> findById(UUID id);

    Candidatura saveCandidatura(Candidatura candidatura);
    Optional<Candidatura> findCandidaturaById(UUID id);
    List<Candidatura> findCandidaturasByInquilinoId(UUID inquilinoId);
}
