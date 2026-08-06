package br.com.imoveis.application.fakes;

import br.com.imoveis.application.ports.ImovelRepository;
import br.com.imoveis.domain.imovel.Conta;
import br.com.imoveis.domain.imovel.ContaStatus;
import br.com.imoveis.domain.imovel.Imovel;
import br.com.imoveis.domain.imovel.Unidade;

import java.time.LocalDate;
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
        List<UUID> unidadeIds = unidades.values().stream()
            .filter(u -> u.imovelId().equals(imovelId))
            .map(Unidade::id)
            .toList();
        return new ArrayList<>(contas.values().stream().filter(c -> unidadeIds.contains(c.unidadeId())).toList());
    }
    @Override public List<Conta> findContasParaAlerta(LocalDate hoje, LocalDate limite) {
        return contas.values().stream()
            .filter(c -> c.status() == ContaStatus.PENDENTE)
            .filter(c -> !c.alertaEnviado())
            .filter(c -> !c.vencimento().isBefore(hoje) && !c.vencimento().isAfter(limite))
            .toList();
    }
}
