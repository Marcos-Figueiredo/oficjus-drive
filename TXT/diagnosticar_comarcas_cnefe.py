"""Verificar se as cidades das comarcas problemáticas são municípios válidos no CNEFE."""
import psycopg2

conn = psycopg2.connect(host='localhost', port=5433, user='postgres', password='postgres', dbname='oficiojus_drive')
cur = conn.cursor()

# 1. Ver cidades de ITAMOJI (0329)
print("Comarca 0329 ITAMOJI:")
cur.execute("SELECT cidade FROM cnj_comarcas_cidades WHERE comarca_oooo = '0329'")
for r in cur.fetchall():
    cidade = r[0].upper()
    cur.execute("SELECT COUNT(*) FROM cnefe_unificada WHERE cidade = %s", (cidade,))
    total = cur.fetchone()[0]
    # Tenta com LIKE
    cur.execute("SELECT COUNT(*) FROM cnefe_unificada WHERE cidade LIKE %s", (f'%{cidade[:6]}%',))
    like = cur.fetchone()[0]
    print(f"  {cidade}: exato={total}, like={like}")

# 2. Ver cidades de PASSA-QUATRO (0476)
print("\nComarca 0476 PASSA-QUATRO:")
cur.execute("SELECT cidade FROM cnj_comarcas_cidades WHERE comarca_oooo = '0476'")
for r in cur.fetchall():
    cidade = r[0].upper()
    cur.execute("SELECT COUNT(*) FROM cnefe_unificada WHERE cidade = %s", (cidade,))
    total = cur.fetchone()[0]
    cur.execute("SELECT COUNT(*) FROM cnefe_unificada WHERE cidade LIKE %s", (f'%{cidade[:6]}%',))
    like = cur.fetchone()[0]
    print(f"  {cidade}: exato={total}, like={like}")

# 3. Ver cidades de GUANHÃES (0280)
print("\nComarca 0280 GUANHÃES:")
cur.execute("SELECT cidade FROM cnj_comarcas_cidades WHERE comarca_oooo = '0280'")
for r in cur.fetchall():
    cidade = r[0].upper()
    cur.execute("SELECT COUNT(*) FROM cnefe_unificada WHERE cidade = %s", (cidade,))
    total = cur.fetchone()[0]
    cur.execute("SELECT COUNT(*) FROM cnefe_unificada WHERE cidade LIKE %s", (f'%{cidade[:6]}%',))
    like = cur.fetchone()[0]
    print(f"  {cidade}: exato={total}, like={like}")

# 4. Ver todas as comarcas com cidades que NÃO são municípios no CNEFE
print("\nComarcas com cidades que não existem no CNEFE:")
cur.execute("""
    SELECT DISTINCT c.comarca_oooo, k.nome, c.cidade
    FROM cnj_comarcas_cidades c
    JOIN cnj_comarcas k ON k.codigo = c.comarca_oooo AND k.tribunal_codigo = '13'
    WHERE c.tribunal_codigo = '13'
      AND NOT EXISTS (SELECT 1 FROM cnefe_unificada u WHERE u.cidade = c.cidade)
    ORDER BY c.comarca_oooo
""")
for r in cur.fetchall():
    print(f"  {r[0]} {r[1]} -> {r[2]}")

# 5. Verificar quantas comarcas têm pelo menos 1 cidade no CNEFE
print("\nComarcas com pelo menos 1 cidade no CNEFE:")
cur.execute("""
    SELECT COUNT(DISTINCT c.comarca_oooo)
    FROM cnj_comarcas_cidades c
    JOIN cnj_comarcas k ON k.codigo = c.comarca_oooo AND k.tribunal_codigo = '13'
    WHERE c.tribunal_codigo = '13'
      AND EXISTS (SELECT 1 FROM cnefe_unificada u WHERE u.cidade = c.cidade)
""")
print(f"  {cur.fetchone()[0]}")

cur.close()
conn.close()