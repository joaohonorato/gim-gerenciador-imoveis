-- Verificação de e-mail não-bloqueante: a conta continua utilizável mesmo
-- sem confirmar (é só um sinalizador exibido no perfil, ver ConfirmarVerificacaoEmail).
-- Default false pra linhas existentes; contas já em uso continuam funcionando
-- normalmente, só aparecem como "não verificado" até o usuário confirmar.
ALTER TABLE contas_acesso ADD COLUMN email_verificado boolean NOT NULL DEFAULT false;
