-- Telefone do proprietário, opcional — mesmo padrão do cadastro básico em
-- V7__proprietarios_cpf_cnpj_opcional.sql: nada aqui é exigido no registro,
-- só preenchido depois via PUT /auth/me/telefone quando o proprietário
-- quiser. CRECI/razão social ficam pra depois, fora do escopo desta coluna.
ALTER TABLE proprietarios ADD COLUMN telefone character varying(20);
