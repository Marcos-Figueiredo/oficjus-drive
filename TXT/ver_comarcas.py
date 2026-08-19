"""Verificar estrutura das tabelas de comarcas no Supabase."""
import psycopg2
conn = psycopg2.connect(host='aws-0-us-west-2.pooler.supabase.com', port=5432, dbname='postgres',
    user='postgres.weaqkaaqalvpbxkxrfee', password='Senha14498620@', connect_timeout=30)
cur = conn.cursor()

for tabela in ['cnj_comarcas', 'cnj_comarcas_cidades']:
    print(f"\n=== {tabela} ===")
    cur.execute(f"SELECT column_name, data_type FROM information_schema.columns WHERE table_name='{tabela}' ORDER BY ordinal_position")
    for r in cur.fetchall():
        print(f"  {r[0]:20s} {r[1]}")
    cur.execute(f"SELECT * FROM {tabela} LIMIT 3")
    for r in cur.fetchall():
        print(f"  {r}")

cur.close()
conn.close()