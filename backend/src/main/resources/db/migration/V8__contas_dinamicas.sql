-- "Contas" deixa de usar um tipo fixo (IPTU/CONDOMINIO/AGUA/LUZ) e passa a
-- usar um catálogo dinâmico por proprietário, reutilizável em qualquer
-- imóvel dele. A cobrança em si passa a ser por UNIDADE (não mais pelo
-- imóvel inteiro), e ganha um responsável (proprietário ou inquilino) —
-- quando é do inquilino, entra na conta do que ele deve pagar no mês,
-- junto com o aluguel.

CREATE TABLE tipos_conta (
    id uuid NOT NULL,
    proprietario_id uuid NOT NULL,
    nome character varying(120) NOT NULL,
    criado_em timestamp(6) with time zone NOT NULL,
    CONSTRAINT tipos_conta_pkey PRIMARY KEY (id),
    CONSTRAINT fk_tipos_conta_proprietario FOREIGN KEY (proprietario_id) REFERENCES proprietarios(id),
    CONSTRAINT uk_tipos_conta_proprietario_nome UNIQUE (proprietario_id, nome)
);

-- Backfill: um TipoConta por (proprietário, valor do enum antigo) que
-- realmente aparece em contas já existentes.
INSERT INTO tipos_conta (id, proprietario_id, nome, criado_em)
SELECT gen_random_uuid(), i.proprietario_id, c.tipo, now()
FROM contas c
JOIN imoveis i ON i.id = c.imovel_id
GROUP BY i.proprietario_id, c.tipo;

ALTER TABLE contas ADD COLUMN unidade_id uuid;
ALTER TABLE contas ADD COLUMN tipo_conta_id uuid;
ALTER TABLE contas ADD COLUMN responsavel character varying(20) NOT NULL DEFAULT 'PROPRIETARIO';
ALTER TABLE contas ADD COLUMN contrato_id uuid;

-- Backfill: cada conta existente vai pra unidade padrão do imóvel em que
-- estava (hoje só existe 1 unidade por imóvel) e pro tipo_conta_id
-- correspondente ao tipo antigo, dentro do proprietário certo.
UPDATE contas c SET
  unidade_id = (SELECT u.id FROM unidades u WHERE u.imovel_id = c.imovel_id AND u.padrao = true LIMIT 1),
  tipo_conta_id = (
    SELECT tc.id FROM tipos_conta tc
    JOIN imoveis i ON i.id = c.imovel_id
    WHERE tc.proprietario_id = i.proprietario_id AND tc.nome = c.tipo
  );

ALTER TABLE contas ALTER COLUMN unidade_id SET NOT NULL;
ALTER TABLE contas ALTER COLUMN tipo_conta_id SET NOT NULL;
ALTER TABLE contas ALTER COLUMN responsavel DROP DEFAULT;

ALTER TABLE contas ADD CONSTRAINT contas_responsavel_check CHECK (((responsavel)::text = ANY ((ARRAY['PROPRIETARIO'::character varying, 'INQUILINO'::character varying])::text[])));
ALTER TABLE contas ADD CONSTRAINT fk_contas_unidade FOREIGN KEY (unidade_id) REFERENCES unidades(id);
ALTER TABLE contas ADD CONSTRAINT fk_contas_tipo_conta FOREIGN KEY (tipo_conta_id) REFERENCES tipos_conta(id);
ALTER TABLE contas ADD CONSTRAINT fk_contas_contrato FOREIGN KEY (contrato_id) REFERENCES contratos(id);

ALTER TABLE contas DROP CONSTRAINT contas_tipo_check;
ALTER TABLE contas DROP COLUMN tipo;
ALTER TABLE contas DROP COLUMN imovel_id;
