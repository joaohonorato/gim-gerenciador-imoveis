package br.com.imoveis.application.ports;

import br.com.imoveis.domain.auth.ContaAcesso;
import br.com.imoveis.domain.shared.Email;

import java.util.Optional;
import java.util.UUID;

public interface ContaAcessoRepository {
    ContaAcesso save(ContaAcesso contaAcesso);
    Optional<ContaAcesso> findByEmail(Email email);
    Optional<ContaAcesso> findByEmailPendente(Email email);
    Optional<ContaAcesso> findById(UUID id);
    Optional<ContaAcesso> findByProprietarioId(UUID proprietarioId);
    Optional<ContaAcesso> findByInquilinoId(UUID inquilinoId);
}