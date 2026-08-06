-- Trilha de auditoria mínima pros fluxos de convite (locação e onboarding de
-- proprietário) — rank 5 do backlog técnico. Append-only, sem FK pra manter
-- o registro mesmo que a entidade original seja removida no futuro;
-- entidade_tipo + entidade_id localizam o convite/conviteAcesso dono do
-- evento (mesmo padrão de "chave polimórfica" já usado em arquivos).

CREATE TABLE eventos_auditoria (
    id uuid NOT NULL,
    entidade_tipo character varying(30) NOT NULL,
    entidade_id uuid NOT NULL,
    tipo_evento character varying(50) NOT NULL,
    detalhe text,
    criado_em timestamp(6) with time zone NOT NULL,
    CONSTRAINT eventos_auditoria_pkey PRIMARY KEY (id)
);

CREATE INDEX idx_eventos_auditoria_entidade ON eventos_auditoria(entidade_tipo, entidade_id);
