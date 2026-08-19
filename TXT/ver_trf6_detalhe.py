"""Verificar detalhadamente subseções TRF6 no Supabase."""
import psycopg2

conn = psycopg2.connect(host='aws-0-us-west-2.pooler.supabase.com', port=5432, dbname='postgres',
    user='postgres.weaqkaaqalvpbxkxrfee', password='Senha14498620@', connect_timeout=30)
cur = conn.cursor()

# Todas comarcas TRF6 (ativas e inativas)
cur.execute("SELECT * FROM cnj_comarcas WHERE tribunal_codigo = '06' ORDER BY codigo")
all_trf6 = cur.fetchall()
print(f"Todas comarcas TRF6: {len(all_trf6)}")
for r in all_trf6:
    print(f"  id={r[0]} seg={r[1]} trib={r[2]} cod={r[3]} nome={r[4]} ordem={r[5]} ativo={r[6]}")

# Ver colunas completas
cur.execute("SELECT column_name, data_type FROM information_schema.columns WHERE table_name = 'cnj_comarcas' ORDER BY ordinal_position")
print("\nColunas cnj_comarcas:")
for r in cur.fetchall():
    print(f"  {r[0]} ({r[1]})")

# Ver cnj_origens_judiciarias (talvez tenha mais dados TRF6)
cur.execute("SELECT column_name, data_type FROM information_schema.columns WHERE table_name = 'cnj_origens_judiciarias' ORDER BY ordinal_position")
cols = cur.fetchall()
print(f"\ncnj_origens_judiciarias ({len(cols)} cols):")
for r in cols:
    print(f"  {r[0]} ({r[1]})")

# Ver se tem TRF6 em origens
cur.execute("SELECT * FROM cnj_origens_judiciarias LIMIT 5")
print("\nAmostra cnj_origens_judiciarias:")
for r in cur.fetchall():
    print(f"  {r}")

# Ver cnj_segmentos
cur.execute("SELECT * FROM cnj_segmentos")
print("\nSegmentos:")
for r in cur.fetchall():
    print(f"  {r}")

# Ver total de cidades por subseção TRF6
cur.execute("""
    SELECT c.codigo, c.nome, COUNT(cid.cidade) as n_cidades
    FROM cnj_comarcas c
    LEFT JOIN cnj_comarcas_cidades cid ON cid.comarca_oooo = c.codigo AND cid.tribunal_codigo = '06'
    WHERE c.tribunal_codigo = '06' AND c.ativo = true
    GROUP BY c.codigo, c.nome
    ORDER BY c.codigo
""")
print("\nSubseções com nº de cidades:")
for r in cur.fetchall():
    print(f"  {r[0]} {r[1]}: {r[2]} cidades")

cur.close()
conn.close()