package br.com.imoveis.domain.auditoria;

// Compartilhado entre CONVITE e CONVITE_ACESSO — nem todo valor se aplica a
// ambas as entidades (ex.: RECUSADO só existe pro convite de locação), mesmo
// padrão de enum compartilhado já usado em outros pontos do domínio
// (ParteContrato reaproveitado entre Conta e Contrato).
public enum TipoEventoAuditoria {
    CRIADO,
    ENVIADO,
    RENOVADO,
    CANDIDATURA_CRIADA,
    CONSUMIDO,
    ACEITO,
    REVOGADO,
    RECUSADO,
    TENTATIVA_COM_TOKEN_EXPIRADO,
    TENTATIVA_COM_TOKEN_INVALIDO_OU_CONSUMIDO
}
