-- Alertas proativos de vencimento (contrato, garantia, conta) + registro de
-- push token por conta de acesso. Cada flag "alerta_*_enviado" evita reenvio
-- duplicado e torna o job agendado tolerante a dias em que ele não rodou: a
-- consulta usa uma janela (hoje..limite), não um dia exato, então um item
-- que "passou" pela janela sem o job rodar ainda é pego no próximo run,
-- desde que a flag continue false.

ALTER TABLE contratos ADD COLUMN alerta_vencimento_enviado boolean NOT NULL DEFAULT false;
ALTER TABLE contratos ADD COLUMN alerta_garantia_enviado boolean NOT NULL DEFAULT false;
ALTER TABLE contas ADD COLUMN alerta_enviado boolean NOT NULL DEFAULT false;

CREATE TABLE push_tokens (
    id uuid NOT NULL,
    conta_acesso_id uuid NOT NULL,
    token character varying(255) NOT NULL,
    criado_em timestamp(6) with time zone NOT NULL,
    CONSTRAINT push_tokens_pkey PRIMARY KEY (id),
    CONSTRAINT fk_push_tokens_conta_acesso FOREIGN KEY (conta_acesso_id) REFERENCES contas_acesso(id),
    CONSTRAINT uk_push_tokens_token UNIQUE (token)
);

CREATE INDEX idx_push_tokens_conta_acesso ON push_tokens(conta_acesso_id);
