package br.com.imoveis.application.ports;

import br.com.imoveis.domain.imovel.Conta;
import br.com.imoveis.domain.imovel.Imovel;
import br.com.imoveis.domain.imovel.Unidade;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ImovelRepository {
    Imovel save(Imovel imovel);
    Optional<Imovel> findById(UUID id);
    List<Imovel> findByProprietario(UUID proprietarioId);

    Unidade saveUnidade(Unidade unidade);
    Optional<Unidade> findUnidadeById(UUID id);

    Conta saveConta(Conta conta);
    Optional<Conta> findContaById(UUID id);
    List<Conta> findContasByImovel(UUID imovelId);

    // Candidatas ao job diário de alertas de vencimento (ver
    // application/usecase/NotificarContasProximasDoVencimento) — janela
    // [hoje, limite], pendentes, ainda sem alerta enviado.
    List<Conta> findContasParaAlerta(java.time.LocalDate hoje, java.time.LocalDate limite);
}
