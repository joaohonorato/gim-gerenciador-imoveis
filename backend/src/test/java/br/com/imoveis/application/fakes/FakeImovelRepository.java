package br.com.imoveis.application.fakes;

import br.com.imoveis.application.ports.ImovelRepository;
import br.com.imoveis.domain.imovel.Conta;
import br.com.imoveis.domain.imovel.Imovel;
import br.com.imoveis.domain.imovel.Unidade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class FakeImovelRepository implements ImovelRepository {
    public final Map<UUID, Imovel> imoveis = new HashMap<>();
    public final Map<UUID, Unidade> unidades = new HashMap<>();
    public final Map<UUID, Conta> contas = new HashMap<>();

    @Override public Imovel save(Imovel i) {
        imoveis.put(i.id(), i);
        i.unidades().forEach(u -> unidades.putIfAbsent(u.id(), u));
        return i;
    }
    @Override public Optional<Imovel> findById(UUID id) { return Optional.ofNullable(imoveis.get(id)); }
    @Override public List<Imovel> findByProprietario(UUID pid) {
        return imoveis.values().stream().filter(i -> i.proprietarioId().equals(pid)).toList();
    }
    @Override public Unidade saveUnidade(Unidade u) { unidades.put(u.id(), u); return u; }
    @Override public Optional<Unidade> findUnidadeById(UUID id) { return Optional.ofNullable(unidades.get(id)); }
    @Override public Conta saveConta(Conta c) { contas.put(c.id(), c); return c; }
    @Override public Optional<Conta> findContaById(UUID id) { return Optional.ofNullable(contas.get(id)); }
    @Override public List<Conta> findContasByImovel(UUID imovelId) {
        return new ArrayList<>(contas.values().stream().filter(c -> c.imovelId().equals(imovelId)).toList());
    }
}
