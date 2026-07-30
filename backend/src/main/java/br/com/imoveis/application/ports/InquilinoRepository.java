package br.com.imoveis.application.ports;

import br.com.imoveis.domain.inquilino.Inquilino;
import br.com.imoveis.domain.shared.Cpf;
import br.com.imoveis.domain.shared.Email;

import java.util.Optional;
import java.util.UUID;

public interface InquilinoRepository {
    Inquilino save(Inquilino inquilino);
    Optional<Inquilino> findById(UUID id);
    Optional<Inquilino> findByCpf(Cpf cpf);
    Optional<Inquilino> findByEmail(Email email);
}
