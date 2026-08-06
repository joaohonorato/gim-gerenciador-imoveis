-- Cadastro básico: CPF/CNPJ deixa de ser obrigatório no registro do
-- proprietário; passa a ser exigido só na hora de assinar contrato
-- ("cadastro completo"). A constraint UNIQUE permanece — Postgres trata
-- múltiplos NULL como distintos, não colide entre proprietários sem
-- cpf_cnpj preenchido.
ALTER TABLE proprietarios ALTER COLUMN cpf_cnpj DROP NOT NULL;
