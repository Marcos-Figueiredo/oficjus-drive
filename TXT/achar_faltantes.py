"""Descobrir quais comarcas TJMG estão faltando no bucket."""
import psycopg2
from urllib.request import Request, urlopen
import json

SUPABASE_URL = "https://weaqkaaqalvpbxkxrfee.supabase.co"
SUPABASE_ANON_KEY = "sb_publishable_K1A9Oo6uXRAmIXDATMThEw_jDy1fLAJ"
headers = {
    "apikey": SUPABASE_ANON_KEY,
    "Authorization": f"Bearer {SUPABASE_ANON_KEY}",
    "Content-Type": "application/json"
}

# 1. Buscar comarcas no banco
conn = psycopg2.connect(host='localhost', port=5433, user='postgres', password='postgres', dbname='oficiojus_drive')
cur = conn.cursor()
cur.execute("SELECT codigo, nome FROM cnj_comarcas ORDER BY codigo")
comarcas = {r[0]: r[1] for r in cur.fetchall()}
cur.close()
conn.close()

# 2. Listar pastas no bucket
body = json.dumps({"prefix": "MG/TJMG/", "limit": 1000}).encode()
req = Request(f"{SUPABASE_URL}/storage/v1/object/list/cnefe-data", data=body, headers=headers, method="POST")
resp = urlopen(req)
arquivos = json.loads(resp.read())

pastas_bucket = set()
for a in arquivos:
    if a and a.get('name'):
        name = a['name'].rstrip('/')
        if name:
            pastas_bucket.add(name)

# 3. Comparar
print(f"Comarcas no banco: {len(comarcas)}")
print(f"Pastas no bucket:  {len(pastas_bucket)}")
print()

faltantes = sorted(set(comarcas.keys()) - pastas_bucket)
print(f"Faltantes ({len(faltantes)}):")
for cod in faltantes:
    print(f"  {cod} {comarcas[cod]}")

# 4. Verificar se geraram 0 registros no export
print()
for cod in faltantes:
    conn2 = psycopg2.connect(host='localhost', port=5433, user='postgres', password='postgres', dbname='oficiojus_drive')
    cur2 = conn2.cursor()
    # Buscar cidades da comarca
    cur2.execute("SELECT cidade FROM cnj_comarcas_cidades WHERE comarca_oooo = %s", (cod,))
    cidades = [r[0].upper() for r in cur2.fetchall()]
    if cidades:
        placeholders = ','.join(['%s'] * len(cidades))
        cur2.execute(f"SELECT COUNT(*) FROM cnefe_unificada WHERE cidade IN ({placeholders})", cidades)
        total = cur2.fetchone()[0]
        print(f"  {cod} {comarcas[cod]}: {len(cidades)} cidades, {total} registros no CNEFE")
    else:
        print(f"  {cod} {comarcas[cod]}: SEM CIDADES no mapeamento")
    cur2.close()
    conn2.close()