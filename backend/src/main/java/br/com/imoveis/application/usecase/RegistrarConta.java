package br.com.imoveis.application.usecase;

import br.com.imoveis.application.exception.NaoEncontradoException;
import br.com.imoveis.application.ports.ContratoRepository;
import br.com.imoveis.application.ports.ImovelRepository;
import br.com.imoveis.application.ports.TipoContaRepository;
import br.com.imoveis.domain.contrato.Contrato;
import br.com.imoveis.domain.contrato.ParteContrato;
import br.com.imoveis.domain.imovel.Conta;
import br.com.imoveis.domain.imovel.Imovel;
import br.com.imoveis.domain.imovel.TipoConta;
import br.com.imoveis.domain.shared.Dinheiro;
import jakarta.inject.Singleton;

import java.time.LocalDate;
import java.util.UUID;

@Singleton
public class RegistrarConta {

    private final ImovelRepository imovelRepository;
    private final TipoContaRepository tipoContaRepository;
    private final ContratoRepository contratoRepository;

    public RegistrarConta(ImovelRepository imovelRepository, TipoContaRepository tipoContaRepository,
                           ContratoRepository contratoRepository) {
        this.imovelRepository = imovelRepository;
        this.tipoContaRepository = tipoContaRepository;
        this.contratoRepository = contratoRepository;
    }

    public Conta execute(UUID imovelId, UUID proprietarioId, UUID tipoContaId, LocalDate vencimento,
                          Dinheiro valor, ParteContrato responsavel, UUID contratoId) {
        Imovel imovel = imovelRepository.findById(imovelId)
            .filter(i -> i.proprietarioId().equals(proprietarioId))
            .orElseThrow(() -> new NaoEncontradoException("imóvel"));
        UUID unidadeId = imovel.unidadePadrao().id();

        TipoConta tipoConta = tipoContaRepository.findById(tipoContaId)
            .filter(tc -> tc.proprietarioId().equals(proprietarioId))
            .orElseThrow(() -> new NaoEncontradoException("tipo de conta"));

        if (responsavel == ParteContrato.INQUILINO) {
            if (contratoId == null) {
                throw new IllegalArgumentException("contratoId é obrigatório quando responsavel é INQUILINO");
            }
            Contrato contrato = contratoRepository.findById(contratoId)
                .orElseThrow(() -> new NaoEncontradoException("contrato"));
            if (!contrato.unidadeId().equals(unidadeId) || !contrato.proprietarioId().equals(proprietarioId)) {
                throw new NaoEncontradoException("contrato");
            }
        }

        Conta conta = Conta.registrar(unidadeId, tipoConta.id(), vencimento, valor, responsavel, contratoId);
        return imovelRepository.saveConta(conta);
    }
}
