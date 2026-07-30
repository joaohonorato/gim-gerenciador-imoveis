package br.com.imoveis.infrastructure.auth;

import br.com.imoveis.application.exception.AutenticacaoInvalidaException;
import br.com.imoveis.domain.auth.TipoContaAcesso;

import java.util.UUID;

public record Principal(UUID contaAcessoId, TipoContaAcesso tipoConta, UUID proprietarioId, UUID inquilinoId) {
	public UUID requireProprietarioId() {
		if (proprietarioId == null) {
			throw new AutenticacaoInvalidaException("conta sem acesso de proprietário");
		}
		return proprietarioId;
	}

	public UUID requireInquilinoId() {
		if (inquilinoId == null) {
			throw new AutenticacaoInvalidaException("conta sem acesso de inquilino");
		}
		return inquilinoId;
	}
}
