-- Links a contrato back to the convite it originated from, so the convite can be
-- marked CONSUMIDO once the contrato is fully signed by both parties. Nullable:
-- contratos created before this migration have no known origin convite.
ALTER TABLE contratos ADD COLUMN convite_id uuid;
