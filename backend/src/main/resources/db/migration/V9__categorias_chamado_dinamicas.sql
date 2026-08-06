-- "Categoria de chamado" deixa de usar um enum fixo (ELETRICA/HIDRAULICA/
-- ESTRUTURAL/OUTRO) e passa a usar um catálogo dinâmico por proprietário,
-- mesmo padrão já adotado por "tipo de conta" em V8. Também aproveita esta
-- migration pra semear um catálogo padrão (tipos de conta e categorias de
-- chamado) em todo proprietário que já existe hoje — daqui pra frente,
-- proprietários novos recebem esse mesmo catálogo padrão na criação (ver
-- SemearCatalogosPadrao).

CREATE TABLE categorias_chamado (
    id uuid NOT NULL,
    proprietario_id uuid NOT NULL,
    nome character varying(120) NOT NULL,
    criado_em timestamp(6) with time zone NOT NULL,
    CONSTRAINT categorias_chamado_pkey PRIMARY KEY (id),
    CONSTRAINT fk_categorias_chamado_proprietario FOREIGN KEY (proprietario_id) REFERENCES proprietarios(id),
    CONSTRAINT uk_categorias_chamado_proprietario_nome UNIQUE (proprietario_id, nome)
);

-- Semeia o catálogo padrão pra todo proprietário que já existe ANTES do
-- backfill de chamados abaixo, pra que o backfill só precise resolver o
-- valor antigo do enum pro nome "bonito" equivalente já semeado aqui — sem
-- isso, um chamado com categoria='HIDRAULICA' criaria uma linha separada de
-- "Hidráulica" (nomes tecnicamente diferentes por causa de acento/caixa).
INSERT INTO tipos_conta (id, proprietario_id, nome, criado_em)
SELECT gen_random_uuid(), p.id, defaults.nome, now()
FROM proprietarios p
CROSS JOIN (VALUES ('Luz'), ('Água'), ('IPTU'), ('Condomínio')) AS defaults(nome)
WHERE NOT EXISTS (
    SELECT 1 FROM tipos_conta tc WHERE tc.proprietario_id = p.id AND tc.nome = defaults.nome
);

INSERT INTO categorias_chamado (id, proprietario_id, nome, criado_em)
SELECT gen_random_uuid(), p.id, defaults.nome, now()
FROM proprietarios p
CROSS JOIN (VALUES ('Elétrica'), ('Hidráulica'), ('Estrutural'), ('Outro')) AS defaults(nome)
WHERE NOT EXISTS (
    SELECT 1 FROM categorias_chamado cc WHERE cc.proprietario_id = p.id AND cc.nome = defaults.nome
);

ALTER TABLE chamados ADD COLUMN categoria_id uuid;

-- Backfill: cada chamado existente aponta pra categoria_id correspondente,
-- mapeando o valor antigo do enum pro nome "bonito" já semeado acima (o
-- enum só tinha esses 4 valores possíveis, garantido pelo CHECK antigo).
UPDATE chamados ch SET
  categoria_id = (
    SELECT cc.id FROM categorias_chamado cc
    JOIN imoveis i ON i.id = ch.imovel_id
    WHERE cc.proprietario_id = i.proprietario_id
      AND cc.nome = CASE ch.categoria
        WHEN 'ELETRICA' THEN 'Elétrica'
        WHEN 'HIDRAULICA' THEN 'Hidráulica'
        WHEN 'ESTRUTURAL' THEN 'Estrutural'
        WHEN 'OUTRO' THEN 'Outro'
      END
  );

ALTER TABLE chamados ALTER COLUMN categoria_id SET NOT NULL;
ALTER TABLE chamados ADD CONSTRAINT fk_chamados_categoria FOREIGN KEY (categoria_id) REFERENCES categorias_chamado(id);

ALTER TABLE chamados DROP CONSTRAINT chamados_categoria_check;
ALTER TABLE chamados DROP COLUMN categoria;
