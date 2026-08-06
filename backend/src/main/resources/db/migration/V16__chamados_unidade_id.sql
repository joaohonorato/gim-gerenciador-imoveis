-- Chamados passam a apontar pra uma unidade específica do imóvel, não mais
-- pro imóvel inteiro — mesmo motivo/padrão de V8__contas_dinamicas.sql: um
-- imóvel pode ter várias unidades (ex.: terreno com várias casas, cada uma
-- com vários apartamentos) e um chamado é sobre uma delas, não sobre o
-- imóvel como um todo.
ALTER TABLE chamados ADD COLUMN unidade_id uuid;

-- Backfill: cada chamado existente vai pra unidade padrão do imóvel em que
-- foi aberto (até aqui só existia 1 unidade por imóvel).
UPDATE chamados c SET
  unidade_id = (SELECT u.id FROM unidades u WHERE u.imovel_id = c.imovel_id AND u.padrao = true LIMIT 1);

ALTER TABLE chamados ALTER COLUMN unidade_id SET NOT NULL;
ALTER TABLE chamados ADD CONSTRAINT fk_chamados_unidade FOREIGN KEY (unidade_id) REFERENCES unidades(id);
