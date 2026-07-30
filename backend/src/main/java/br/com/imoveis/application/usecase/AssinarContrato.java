package br.com.imoveis.application.usecase;

import br.com.imoveis.application.exception.NaoEncontradoException;
import br.com.imoveis.application.ports.ContratoRepository;
import br.com.imoveis.application.ports.ImovelRepository;
import br.com.imoveis.domain.contrato.Contrato;
import br.com.imoveis.domain.contrato.ContratoAssinaturaStatus;
import br.com.imoveis.domain.contrato.Pagamento;
import br.com.imoveis.domain.contrato.ParteContrato;
import br.com.imoveis.domain.imovel.Unidade;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;

import java.util.UUID;

@Singleton
@Transactional
public class AssinarContrato {

    private final ContratoRepository contratoRepository;
    private final ImovelRepository imovelRepository;

    public AssinarContrato(ContratoRepository contratoRepository, ImovelRepository imovelRepository) {
        this.contratoRepository = contratoRepository;
        this.imovelRepository = imovelRepository;
    }

    public Contrato execute(UUID contratoId, ParteContrato parte) {
        Contrato contrato = contratoRepository.findById(contratoId)
            .orElseThrow(() -> new NaoEncontradoException("contrato"));

        contrato.assinar(parte);

        Contrato salvo = contratoRepository.save(contrato);
        if (salvo.statusAssinatura() == ContratoAssinaturaStatus.ASSINADO) {
            for (Pagamento p : salvo.pagamentos()) {
                contratoRepository.savePagamento(p);
            }
            Unidade unidade = imovelRepository.findUnidadeById(salvo.unidadeId())
                .orElseThrow(() -> new NaoEncontradoException("unidade"));
            unidade.alugar();
            imovelRepository.saveUnidade(unidade);
        }
        return salvo;
    }
}
