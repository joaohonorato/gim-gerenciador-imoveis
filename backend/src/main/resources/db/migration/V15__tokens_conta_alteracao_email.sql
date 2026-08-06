-- Nova finalidade de tokens_conta pra confirmar uma troca de e-mail (ver
-- V14). Discriminada de VERIFICACAO_EMAIL porque o lookup da conta na
-- confirmação é diferente: VERIFICACAO_EMAIL busca por contas_acesso.email
-- (e-mail atual, ainda não confirmado logo após o cadastro); ALTERACAO_EMAIL
-- busca por contas_acesso.email_pendente (e-mail novo, ainda não é o ativo).
ALTER TABLE tokens_conta DROP CONSTRAINT tokens_conta_finalidade_check;
ALTER TABLE tokens_conta ADD CONSTRAINT tokens_conta_finalidade_check
    CHECK (finalidade IN ('RESET_SENHA', 'VERIFICACAO_EMAIL', 'ALTERACAO_EMAIL'));
