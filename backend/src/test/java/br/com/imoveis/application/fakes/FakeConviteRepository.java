package br.com.imoveis.application.fakes;

import br.com.imoveis.application.ports.ConviteRepository;
import br.com.imoveis.domain.convite.Candidatura;
import br.com.imoveis.domain.convite.Convite;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class FakeConviteRepository implements ConviteRepository {
    public final Map<UUID, Convite> convites = new HashMap<>();
    public final Map<UUID, Candidatura> candidaturas = new HashMap<>();

    @Override public Convite save(Convite c) { convites.put(c.id(), c); return c; }
    @Override public Optional<Convite> findByToken(String token) {
        return convites.values().stream().filter(c -> c.token().equals(token)).findFirst();
    }
    @Override public Optional<Convite> findById(UUID id) { return Optional.ofNullable(convites.get(id)); }
    @Override public List<Convite> findByImovelId(UUID imovelId) {
        return convites.values().stream().filter(c -> c.imovelId().equals(imovelId)).toList();
    }
    @Override public List<Convite> findByProprietarioId(UUID proprietarioId) {
        return convites.values().stream().filter(c -> c.proprietarioId().equals(proprietarioId)).toList();
    }
    @Override public Candidatura saveCandidatura(Candidatura c) { candidaturas.put(c.id(), c); return c; }
    @Override public Optional<Candidatura> findCandidaturaById(UUID id) { return Optional.ofNullable(candidaturas.get(id)); }
    @Override public List<Candidatura> findCandidaturasByInquilinoId(UUID inquilinoId) {
        return candidaturas.values().stream().filter(c -> c.inquilinoId().equals(inquilinoId)).toList();
    }
    @Override public List<Candidatura> findCandidaturasByProprietarioId(UUID proprietarioId) {
        return candidaturas.values().stream()
            .filter(c -> convites.containsKey(c.conviteId()) && convites.get(c.conviteId()).proprietarioId().equals(proprietarioId))
            .toList();
    }
}
