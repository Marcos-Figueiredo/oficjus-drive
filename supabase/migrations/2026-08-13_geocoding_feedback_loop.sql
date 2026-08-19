-- OficJus Drive — Geocoding Feedback Loop: upsert direto na cnefe_numeros
-- Permite que o app salve coordenadas colhidas em campo (Nominatim + GPS)
-- sem precisar de tabela paralela.

-- 1. Unique index (cep, numero) — permite upsert limpo
--    WHERE cep IS NOT NULL evita conflitos com registros sem CEP
CREATE UNIQUE INDEX IF NOT EXISTS idx_cnefe_numeros_cep_numero
  ON cnefe_numeros (cep, numero)
  WHERE cep IS NOT NULL AND numero IS NOT NULL;

-- 2. RLS: habilitar Row Level Security na tabela (se ainda não estiver)
ALTER TABLE cnefe_numeros ENABLE ROW LEVEL SECURITY;

-- 3. Policy INSERT — app autenticado pode inserir novos registros
CREATE POLICY "geofeedback_insert" ON cnefe_numeros
  FOR INSERT
  WITH CHECK (auth.role() = 'authenticated');

-- 4. Policy UPDATE — app autenticado pode atualizar coordenadas existentes
CREATE POLICY "geofeedback_update" ON cnefe_numeros
  FOR UPDATE
  USING (auth.role() = 'authenticated');