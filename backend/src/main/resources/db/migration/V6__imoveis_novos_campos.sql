-- Campos que definem valor de aluguel/venda de um imóvel — hoje o cadastro só
-- tinha endereço/matrícula, sem nada que sustente uma proposta de aluguel.
-- Tudo nullable: imóveis já cadastrados não têm nenhum desses valores.
ALTER TABLE imoveis
    ADD COLUMN quartos integer,
    ADD COLUMN banheiros integer,
    ADD COLUMN vagas integer,
    ADD COLUMN area_m2 numeric(8,2),
    ADD COLUMN iptu numeric(10,2),
    ADD COLUMN cep varchar(9);
