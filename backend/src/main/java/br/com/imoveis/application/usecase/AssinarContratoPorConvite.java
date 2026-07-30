package br.com.imoveis.application.usecase;

import br.com.imoveis.application.exception.ConflitoException;
import br.com.imoveis.application.exception.NaoEncontradoException;
import br.com.imoveis.application.ports.ConviteRepository;
import br.com.imoveis.application.ports.ContratoRepository;
import br.com.imoveis.domain.contrato.Contrato;
import br.com.imoveis.domain.contrato.ParteContrato;
import br.com.imoveis.domain.convite.Candidatura;
import br.com.imoveis.domain.convite.CandidaturaStatus;
import br.com.imoveis.domain.convite.Convite;
import jakarta.inject.Singleton;

@Singleton
public class AssinarContratoPorConvite {

    private final ConviteRepository conviteRepository;
    private final ContratoRepository contratoRepository;
    private final AssinarContrato assinarContrato;

    public AssinarContratoPorConvite(ConviteRepository conviteRepository,
                                     ContratoRepository contratoRepository,
                                     AssinarContrato assinarContrato) {
        this.conviteRepository = conviteRepository;
        this.contratoRepository = contratoRepository;
        this.assinarContrato = assinarContrato;
    }

    public Contrato execute(String token) {
        Convite convite = conviteRepository.findByToken(token)
            .orElseThrow(() -> new NaoEncontradoException("convite"));
        if (convite.candidaturaId() == null) {
            throw new ConflitoException("convite ainda não possui candidatura aprovada");
        }

        Candidatura candidatura = conviteRepository.findCandidaturaById(convite.candidaturaId())
            .orElseThrow(() -> new NaoEncontradoException("candidatura"));
        if (candidatura.status() != CandidaturaStatus.APROVADA) {
            throw new ConflitoException("candidatura ainda não foi aprovada");
        }

        Contrato contrato = contratoRepository.findByUnidadeInquilinoEPeriodo(
                convite.unidadeId(),
                candidatura.inquilinoId(),
                convite.condicoes().periodoSugerido().inicio(),
                convite.condicoes().periodoSugerido().fim())
            .orElseThrow(() -> new NaoEncontradoException("contrato"));

        return assinarContrato.execute(contrato.id(), ParteContrato.INQUILINO);
    }
}