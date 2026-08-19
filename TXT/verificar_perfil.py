"""Verificar perfis no Supabase."""
import psycopg2

conn = psycopg2.connect(
    host='aws-0-us-west-2.pooler.supabase.com', port=5432,
    dbname='postgres',
    user='postgres.weaqkaaqalvpbxkxrfee',
    password='Senha14498620@',
    connect_timeout=30
)
cur = conn.cursor()

# Colunas
cur.execute("SELECT column_name, data_type, udt_name FROM information_schema.columns WHERE table_name = 'profiles' ORDER BY ordinal_position")
cols = cur.fetchall()
print("Colunas profiles:")
for c in cols:
    print(f"  {c[0]:30s} {c[1]:15s} {c[2]:15s}")

# Perfis
cur.execute("SELECT * FROM profiles")
rows = cur.fetchall()
print(f"\n{len(rows)} perfis:")
for r in rows:
    print(f"  {r}")

cur.close()
conn.close()