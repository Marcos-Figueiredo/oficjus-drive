"""Verificar estado atual das comarcas no Supabase após correção."""
import psycopg2
conn = psycopg2.connect(host='aws-0-us-west-2.pooler.supabase.com', port=5432, dbname='postgres',
    user='postgres.weaqkaaqalvpbxkxrfee', password='Senha14498620@', connect_timeout=30)
cur = conn.cursor()

cur.execute("SELECT codigo, nome FROM cnj_comarcas WHERE tribunal_codigo = '13' ORDER BY codigo")
comarcas = {r[0]: r[1] for r in cur.fetchall()}
print(f"Total: {len(comarcas)}")

for c in ['0252','0627','0738','0740','0352']:
    existe = "SIM" if c in comarcas else "NAO"
    print(f"  {c}: {existe} - {comarcas.get(c, '-')}")

# Verificar se 0352 foi removido
cur.execute("SELECT codigo FROM cnj_comarcas WHERE codigo = '0352'")
print(f"\n0352 existe: {cur.fetchone() is not None}")

# Ver cidades de 0252
cur.execute("SELECT cidade FROM cnj_comarcas_cidades WHERE comarca_oooo = '0252'")
print(f"\nCidades de 0252 Januária: {len(cur.fetchall())}")

cur.close()
conn.close()