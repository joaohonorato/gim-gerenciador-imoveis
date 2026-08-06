-- E-mail pendente de confirmação: alterar o e-mail de login não troca o
-- valor ativo na hora — fica guardado aqui até o usuário clicar no link de
-- confirmação enviado pro *novo* endereço (ver SolicitarAlteracaoEmail /
-- ConfirmarVerificacaoEmail). Login continua pelo e-mail atual
-- (contas_acesso.email) até a confirmação, pra não travar o usuário fora da
-- própria conta por um erro de digitação no novo e-mail.
ALTER TABLE contas_acesso ADD COLUMN email_pendente varchar(255);
