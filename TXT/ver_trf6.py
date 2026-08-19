"""Verificar dados TRF6 no Supabase."""
import psycopg2

conn = psycopg2.connect(host='aws-0-us-west-2.pooler.supabase.com', port=5432, dbname='postgres',
    user='postgres.weaqkaaqalvpbxkxrfee', password='Senha14498620@', connect_timeout=30)
cur = conn.cursor()

# Procurar tabelas com TRF6
cur.execute("""
    SELECT table_name FROM information_schema.tables 
    WHERE table_schema='public' 
    ORDER BY table_name
""")
print("Tabelas:")
for r in cur.fetchall():
    print(f"  {r[0]}")

# Ver tribunais
cur.execute("SELECT * FROM cnj_tribunais")
print("\ncnj_tribunais:")
for r in cur.fetchall():
    print(f"  {r}")

# Ver se existe dados TRF6 em cnj_comarcas
cur.execute("SELECT * FROM cnj_comarcas WHERE tribunal_codigo = '06' LIMIT 20")
rows = cur.fetchall()
print(f"\ncnj_comarcas tribunal 06 (TRF6): {len(rows)}")
for r in rows:
    print(f"  {r}")

# Ver cnj_comarcas_cidades tribunal 06
cur.execute("SELECT * FROM cnj_comarcas_cidades WHERE tribunal_codigo = '06' LIMIT 20")
rows2 = cur.fetchall()
print(f"\ncnj_comarcas_cidades tribunal 06: {len(rows2)}")
for r in rows2:
    print(f"  {r}")

cur.close()
conn.close()