"""Diagnosticar estado atual do Supabase após correções."""
import psycopg2

conn = psycopg2.connect(host='aws-0-us-west-2.pooler.supabase.com', port=5432, dbname='postgres',
    user='postgres.weaqkaaqalvpbxkxrfee', password='Senha14498620@', connect_timeout=30)
cur = conn.cursor()

# Total
cur.execute("SELECT COUNT(*) FROM cnj_comarcas WHERE tribunal_codigo = '13'")
print(f"Total TJMG: {cur.fetchone()[0]}")

# Listar comarcas por código
cur.execute("SELECT codigo, nome FROM cnj_comarcas WHERE tribunal_codigo = '13' ORDER BY codigo")
comarcas = cur.fetchall()
print(f"\nLista ({len(comarcas)}):")
for r in comarcas:
    print(f"  {r[0]} {r[1]}")

# Verificar duplicatas
cur.execute("SELECT codigo, COUNT(*) FROM cnj_comarcas WHERE tribunal_codigo = '13' GROUP BY codigo HAVING COUNT(*) > 1")
dups = cur.fetchall()
if dups:
    print(f"\nDuplicatas: {dups}")
else:
    print(f"\nSem duplicatas ✅")

# Verificar 0252 e 0352
for c in ['0252', '0352', '0627', '0738', '0740']:
    cur.execute("SELECT codigo, nome FROM cnj_comarcas WHERE codigo = %s", (c,))
    r = cur.fetchone()
    print(f"  {c}: {r}")

cur.close()
conn.close()