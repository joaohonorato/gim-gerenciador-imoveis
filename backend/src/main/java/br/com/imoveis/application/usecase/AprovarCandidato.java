package br.com.imoveis.application.usecase;

import br.com.imoveis.application.exception.ConflitoException;
import br.com.imoveis.application.exception.NaoEncontradoException;
import br.com.imoveis.application.ports.ConviteRepository;
import br.com.imoveis.application.ports.ContratoRepository;
import br.com.imoveis.application.ports.ImovelRepository;
import br.com.imoveis.domain.contrato.Contrato;
import br.com.imoveis.domain.contrato.ContratoAssinaturaStatus;
import br.com.imoveis.domain.contrato.GarantiaTipo;
import br.com.imoveis.domain.convite.Candidatura;
import br.com.imoveis.domain.convite.Convite;
import br.com.imoveis.domain.imovel.Unidade;
import br.com.imoveis.domain.shared.Periodo;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;

import java.util.UUID;

@Singleton
@Transactional
public class AprovarCandidato {

    private final ConviteRepository conviteRepository;
    private final ContratoRepository contratoRepository;
    private final ImovelRepository imovelRepository;

    public AprovarCandidato(ConviteRepository conviteRepository,
                             ContratoRepository contratoRepository,
                             ImovelRepository imovelRepository) {
        this.conviteRepository = conviteRepository;
        this.contratoRepository = contratoRepository;
        this.imovelRepository = imovelRepository;
    }

    public Contrato execute(UUID candidaturaId, UUID proprietarioId) {
        Candidatura candidatura = conviteRepository.findCandidaturaById(candidaturaId)
            .orElseThrow(() -> new NaoEncontradoException("candidatura"));
        Convite convite = conviteRepository.findById(candidatura.conviteId())
            .orElseThrow(() -> new NaoEncontradoException("convite"));

        if (!convite.proprietarioId().equals(proprietarioId)) {
            throw new NaoEncontradoException("candidatura");
        }

        Convite.CondicoesConvite condicoes = convite.condicoes();
        Periodo periodo = condicoes.periodoSugerido();

        contratoRepository.findByUnidadeAndStatus(convite.unidadeId(), ContratoAssinaturaStatus.ASSINADO)
            .stream()
            .filter(c -> c.periodo().overlaps(periodo))
            .findAny()
            .ifPresent(c -> {
                throw new ConflitoException("já existe contrato assinado com período sobreposto na unidade");
            });

        Contrato contrato = Contrato.novo(convite.unidadeId(), candidatura.inquilinoId(), proprietarioId,
            periodo, condicoes.tipoContrato(), condicoes.valorAluguel(), "IPCA");

        boolean exigeGarantia = condicoes.garantiaAceita() != null && condicoes.garantiaAceita() != GarantiaTipo.NENHUMA;
        if (exigeGarantia && candidatura.garantiaEscolhida() == null) {
            throw new IllegalStateException("candidatura sem garantia definida");
        }
        if (candidatura.garantiaEscolhida() != null && candidatura.garantiaEscolhida() != GarantiaTipo.NENHUMA) {
            contrato.definirGarantia(candidatura.garantiaEscolhida(), periodo.fim(), candidatura.dadosGarantia());
        }
        contrato = contratoRepository.save(contrato);

        Unidade unidade = imovelRepository.findUnidadeById(convite.unidadeId())
            .orElseThrow(() -> new NaoEncontradoException("unidade"));
        unidade.reservar();
        imovelRepository.saveUnidade(unidade);

        candidatura.aprovar();
        conviteRepository.saveCandidatura(candidatura);

        return contrato;
    }
}
