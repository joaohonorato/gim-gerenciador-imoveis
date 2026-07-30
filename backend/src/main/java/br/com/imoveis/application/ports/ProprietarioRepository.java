package br.com.imoveis.application.ports;

import br.com.imoveis.domain.proprietario.Proprietario;
import br.com.imoveis.domain.shared.CpfCnpj;
import br.com.imoveis.domain.shared.Email;

import java.util.Optional;
import java.util.UUID;

public interface ProprietarioRepository {
    Proprietario save(Proprietario proprietario);
    Optional<Proprietario> findById(UUID id);
    Optional<Proprietario> findByEmail(Email email);
    Optional<Proprietario> findByCpfCnpj(CpfCnpj cpfCnpj);
}
