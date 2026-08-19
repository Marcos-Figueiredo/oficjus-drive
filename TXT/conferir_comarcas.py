"""Conferir contagem exata de comarcas TJMG no Supabase."""
import psycopg2

conn = psycopg2.connect(host='aws-0-us-west-2.pooler.supabase.com', port=5432, dbname='postgres',
    user='postgres.weaqkaaqalvpbxkxrfee', password='Senha14498620@', connect_timeout=30)
cur = conn.cursor()

# Total de comarcas TJMG
cur.execute("SELECT COUNT(*) FROM cnj_comarcas WHERE tribunal_codigo = '13'")
total = cur.fetchone()[0]
print(f"Total comarcas TJMG (todas): {total}")

cur.execute("SELECT COUNT(*) FROM cnj_comarcas WHERE tribunal_codigo = '13' AND ativo = true")
ativas = cur.fetchone()[0]
print(f"Ativas: {ativas}")

cur.execute("SELECT COUNT(*) FROM cnj_comarcas WHERE tribunal_codigo = '13' AND ativo = false")
inativas = cur.fetchone()[0]
print(f"Inativas: {inativas}")

# Verificar comarcas sem cidades (que dariam 0 registros)
cur.execute("""
    SELECT c.codigo, c.nome
    FROM cnj_comarcas c
    LEFT JOIN cnj_comarcas_cidades cid ON cid.comarca_oooo = c.codigo AND cid.tribunal_codigo = '13'
    WHERE c.tribunal_codigo = '13' AND c.ativo = true
    GROUP BY c.codigo, c.nome
    HAVING COUNT(cid.cidade) = 0
    ORDER BY c.codigo
""")
sem_cidades = cur.fetchall()
print(f"\nComarcas ativas SEM cidades: {len(sem_cidades)}")
for r in sem_cidades:
    print(f"  {r[0]} {r[1]}")

# Verificar arquivos no bucket TJMG (comparação)
from urllib.request import Request, urlopen
import json
SUPABASE_URL = "https://weaqkaaqalvpbxkxrfee.supabase.co"
SUPABASE_ANON_KEY = "sb_publishable_K1A9Oo6uXRAmIXDATMThEw_jDy1fLAJ"
headers = {
    "apikey": SUPABASE_ANON_KEY,
    "Authorization": f"Bearer {SUPABASE_ANON_KEY}",
    "Content-Type": "application/json"
}

# Listar arquivos TJMG no bucket
body = json.dumps({"prefix": "MG/TJMG/", "limit": 1000}).encode()
req = Request(f"{SUPABASE_URL}/storage/v1/object/list/cnefe-data", data=body, headers=headers, method="POST")
resp = urlopen(req)
arquivos = json.loads(resp.read())
print(f"\nArquivos no bucket MG/TJMG/: {len(arquivos)}")
# Contar apenas os diretórios (cnefe_unificada.ndjson.gz)
count_files = sum(1 for a in arquivos if a.get('name') == 'cnefe_unificada.ndjson.gz')
print(f"Arquivos cnefe_unificada.ndjson.gz: {count_files}")

# Listar subseções TRF6
cur.execute("SELECT COUNT(*) FROM cnj_comarcas WHERE tribunal_codigo = '06' AND ativo = true")
trf6 = cur.fetchone()[0]
print(f"\nTRF6 subseções ativas: {trf6}")

cur.close()
conn.close()