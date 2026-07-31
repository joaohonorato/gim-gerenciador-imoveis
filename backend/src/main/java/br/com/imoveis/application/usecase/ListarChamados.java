package br.com.imoveis.application.usecase;

import br.com.imoveis.application.ports.ChamadoRepository;
import br.com.imoveis.application.ports.ImovelRepository;
import br.com.imoveis.domain.chamado.Chamado;
import br.com.imoveis.domain.imovel.Imovel;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Singleton
public class ListarChamados {

    private final ChamadoRepository chamadoRepository;
    private final ImovelRepository imovelRepository;

    public ListarChamados(ChamadoRepository chamadoRepository, ImovelRepository imovelRepository) {
        this.chamadoRepository = chamadoRepository;
        this.imovelRepository = imovelRepository;
    }

    public List<Chamado> paraProprietario(UUID proprietarioId, UUID filtroInquilinoId) {
        List<UUID> imovelIds = imovelRepository.findByProprietario(proprietarioId).stream().map(Imovel::id).toList();
        List<Chamado> chamados = chamadoRepository.findByImovelIds(imovelIds);
        if (filtroInquilinoId == null) return chamados;
        return chamados.stream().filter(c -> c.abertoPor().equals(filtroInquilinoId)).toList();
    }

    public List<Chamado> paraInquilino(UUID inquilinoId, UUID filtroProprietarioId) {
        List<Chamado> chamados = chamadoRepository.findByInquilino(inquilinoId);
        if (filtroProprietarioId == null) return chamados;
        Set<UUID> imovelIdsDoProprietario = imovelRepository.findByProprietario(filtroProprietarioId).stream()
            .map(Imovel::id).collect(Collectors.toSet());
        return chamados.stream().filter(c -> imovelIdsDoProprietario.contains(c.imovelId())).toList();
    }
}
