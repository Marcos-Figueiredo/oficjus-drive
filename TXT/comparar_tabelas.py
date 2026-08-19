"""Verificar tabelas no Supabase (fonte da verdade) vs banco local."""
import psycopg2

# Supabase
print("=" * 60)
print("SUPABASE - tabelas")
print("=" * 60)
conn_s = psycopg2.connect(
    host='aws-0-us-west-2.pooler.supabase.com', port=5432,
    dbname='postgres',
    user='postgres.weaqkaaqalvpbxkxrfee',
    password='Senha14498620@',
    connect_timeout=30
)
cur_s = conn_s.cursor()
cur_s.execute("""
    SELECT table_name, (SELECT count(*) FROM information_schema.columns WHERE table_name = t.table_name) as n_cols
    FROM information_schema.tables t
    WHERE table_schema = 'public'
    ORDER BY table_name
""")
tabelas_s = cur_s.fetchall()
for r in tabelas_s:
    print(f"  {r[0]:40s} {r[1]} cols")

# Ver tabelas relacionadas a comarca/trf
for nome in ['comarcas', 'territorios', 'trf6', 'subsecoes', 'municipios']:
    cur_s.execute(f"""
        SELECT table_name FROM information_schema.tables 
        WHERE table_schema='public' AND table_name LIKE '%{nome}%'
    """)
    for r in cur_s.fetchall():
        print(f"  -> Tabela encontrada: {r[0]}")

# Banco local
print()
print("=" * 60)
print("LOCAL (porta 5433) - tabelas")
print("=" * 60)
conn_l = psycopg2.connect(host='localhost', port=5433, user='postgres', password='postgres', dbname='oficiojus_drive')
cur_l = conn_l.cursor()
cur_l.execute("""
    SELECT table_name FROM information_schema.tables 
    WHERE table_schema='public' ORDER BY table_name
""")
for r in cur_l.fetchall():
    print(f"  {r[0]}")

cur_s.close()
conn_s.close()
cur_l.close()
conn_l.close()