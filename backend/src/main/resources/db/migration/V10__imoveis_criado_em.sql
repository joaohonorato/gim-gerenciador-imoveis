-- "Imóvel vago há mais de N dias" (hub de Imóveis) precisa de uma data de
-- referência pra calcular há quanto tempo a unidade está parada. Não existe
-- histórico de quando cada unidade virou VAGO (rastrear isso exigiria tocar
-- em toda transição de status: reservar/alugar/liberar/entrarEmManutencao,
-- espalhadas por vários use cases) — a data de cadastro do imóvel é usada
-- como aproximação, conforme decidido no pedido da feature.
ALTER TABLE imoveis ADD COLUMN criado_em timestamp(6) with time zone;

-- Backfill: imóveis já cadastrados não têm data real de criação registrada
-- em lugar nenhum — usa o momento da migration como aproximação (mesmo
-- efeito de "considerar vago desde sempre" pra quem já está vago hoje).
UPDATE imoveis SET criado_em = now() WHERE criado_em IS NULL;

ALTER TABLE imoveis ALTER COLUMN criado_em SET NOT NULL;
