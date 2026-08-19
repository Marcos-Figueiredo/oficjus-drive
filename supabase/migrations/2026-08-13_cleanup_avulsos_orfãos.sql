-- OficJus Drive — Limpa endereços avulsos órfãos (rotaGrupoId IS NULL)
-- Estes registros foram criados pelo adicionarEndereco() que salvava
-- cada endereço individualmente antes da confirmação da rota.
-- A partir de agora, endereços só são persistidos no confirmarRota().

DELETE FROM enderecos WHERE rotaGrupoId IS NULL;