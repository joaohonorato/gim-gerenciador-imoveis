-- Curto-duração, token-por-e-mail para uma conta de acesso já existente —
-- cobre tanto reset de senha quanto confirmação de e-mail (discriminados por
-- finalidade), mesmo formato de convites_acesso (token+expiração+consumo),
-- mas independente dele: aqui não há convite/onboarding envolvido, é sempre
-- sobre uma conta que já existe.
CREATE TABLE tokens_conta (
    id uuid PRIMARY KEY,
    email varchar(255) NOT NULL,
    token varchar(64) NOT NULL,
    finalidade varchar(30) NOT NULL,
    expira_em timestamptz NOT NULL,
    consumido boolean NOT NULL DEFAULT false,
    criado_em timestamptz NOT NULL,
    CONSTRAINT tokens_conta_token_key UNIQUE (token),
    CONSTRAINT tokens_conta_finalidade_check CHECK (finalidade IN ('RESET_SENHA', 'VERIFICACAO_EMAIL'))
);

CREATE INDEX idx_tokens_conta_email_finalidade ON tokens_conta (email, finalidade);
