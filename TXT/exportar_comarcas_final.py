"""Baixar dados de comarcas do Supabase para o banco local e exportar todas as comarcas."""
import psycopg2, json, time, gzip, io

# Conexões
conn_s = psycopg2.connect(host='aws-0-us-west-2.pooler.supabase.com', port=5432, dbname='postgres',
    user='postgres.weaqkaaqalvpbxkxrfee', password='Senha14498620@', connect_timeout=30)
cur_s = conn_s.cursor()

conn_l = psycopg2.connect(host='localhost', port=5433, user='postgres', password='postgres', dbname='oficiojus_drive')
cur_l = conn_l.cursor()

# 1. Baixar cnj_comarcas_cidades (mapeamento comarca → cidade)
print("1. Baixando cnj_comarcas_cidades do Supabase...", flush=True)
cur_s.execute("SELECT comarca_oooo, cidade, tribunal_codigo FROM cnj_comarcas_cidades")
rows = cur_s.fetchall()
print(f"   {len(rows)} registros", flush=True)

# Criar tabela local
cur_l.execute("DROP TABLE IF EXISTS cnj_comarcas_cidades")
cur_l.execute("""
    CREATE TABLE cnj_comarcas_cidades (
        comarca_oooo TEXT,
        cidade TEXT,
        tribunal_codigo TEXT
    )
""")
for r in rows:
    cur_l.execute("INSERT INTO cnj_comarcas_cidades (comarca_oooo, cidade, tribunal_codigo) VALUES (%s, %s, %s)",
                  (r[0], r[1], r[2]))
conn_l.commit()

# 2. Baixar cnj_comarcas (nomes) — TJMG (tribunal 13)
print("2. Baixando cnj_comarcas do Supabase...", flush=True)
cur_s.execute("SELECT codigo, nome, tribunal_codigo FROM cnj_comarcas WHERE tribunal_codigo = '13' AND ativo = true")
rows2 = cur_s.fetchall()
print(f"   {len(rows2)} registros", flush=True)

cur_l.execute("DROP TABLE IF EXISTS cnj_comarcas")
cur_l.execute("""
    CREATE TABLE cnj_comarcas (
        codigo TEXT,
        nome TEXT,
        tribunal_codigo TEXT
    )
""")
for r in rows2:
    cur_l.execute("INSERT INTO cnj_comarcas (codigo, nome, tribunal_codigo) VALUES (%s, %s, %s)",
                  (r[0], r[1], r[2]))
conn_l.commit()

cur_s.close()
conn_s.close()

# 3. Ver quantas comarcas TJMG
cur_l.execute("SELECT codigo, nome FROM cnj_comarcas ORDER BY codigo")
comarcas = cur_l.fetchall()
print(f"\n3. {len(comarcas)} comarcas TJMG encontradas localmente", flush=True)

# 4. Exportar cada comarca
print("\n4. EXPORTANDO COMARCAS TJMG...", flush=True)
from urllib.request import Request, urlopen
from urllib.error import HTTPError

SUPABASE_URL = "https://weaqkaaqalvpbxkxrfee.supabase.co"
SUPABASE_ANON_KEY = "sb_publishable_K1A9Oo6uXRAmIXDATMThEw_jDy1fLAJ"
BUCKET_NAME = "cnefe-data"

def upload(gzipped, comarca_id):
    path = f"MG/TJMG/{comarca_id}/cnefe_unificada.ndjson.gz"
    req = Request(
        f"{SUPABASE_URL}/storage/v1/object/{BUCKET_NAME}/{path}",
        data=gzipped,
        headers={
            "apikey": SUPABASE_ANON_KEY,
            "Authorization": f"Bearer {SUPABASE_ANON_KEY}",
            "Content-Type": "application/gzip"
        },
        method="PUT"
    )
    try:
        urlopen(req)
        return True
    except HTTPError as e:
        return False

ts_inicio = time.time()
ok = 0
falha = 0

for idx, (comarca_id, comarca_nome) in enumerate(comarcas, 1):
    # Buscar cidades da comarca
    cur_l.execute("SELECT cidade FROM cnj_comarcas_cidades WHERE comarca_oooo = %s", (comarca_id,))
    cidades = [r[0].upper() for r in cur_l.fetchall()]
    if not cidades:
        print(f"[{idx:3d}/{len(comarcas)}] {comarca_id} {comarca_nome}: SEM CIDADES", flush=True)
        falha += 1
        continue

    # Consultar cnefe_unificada local
    ts = time.time()
    # Monta IN clause com placeholders (evita erro com 1 cidade)
    placeholders = ','.join(['%s'] * len(cidades))
    cur_l.execute(f"""
        SELECT id, logradouro_completo, bairro, cep, cidade, estado,
               extensao_metros, menor_numero, maior_numero,
               CASE WHEN geometry IS NULL THEN true ELSE false END as sem_geo,
               numeros_por_cep
        FROM cnefe_unificada
        WHERE cidade IN ({placeholders})
        ORDER BY id
    """, cidades)
    buf = io.StringIO()
    count = 0
    for row in cur_l.fetchall():
        rec = {
            "i": row[0], "l": row[1], "b": row[2], "c": row[3],
            "cid": row[4], "e": row[5], "ext": row[6],
            "mn": row[7], "mx": row[8], "sg": row[9], "n": row[10]
        }
        buf.write(json.dumps(rec, ensure_ascii=False))
        buf.write('\n')
        count += 1
    raw = buf.getvalue()
    buf.close()
    gzipped = gzip.compress(raw.encode('utf-8'), compresslevel=6)
    tamanho_mb = len(gzipped) / 1024 / 1024
    tempo = time.time() - ts

    if count == 0:
        print(f"[{idx:3d}/{len(comarcas)}] {comarca_id} {comarca_nome}: 0 registros", flush=True)
        falha += 1
        continue

    sucesso = upload(gzipped, comarca_id)
    if sucesso:
        ok += 1
        status = "OK  ✅"
    else:
        falha += 1
        status = "ERR ❌"

    print(f"[{idx:3d}/{len(comarcas)}] {comarca_id} {comarca_nome}: {count:>6,} reg, {tamanho_mb:>5.1f} MB, {tempo:>3.0f}s  {status}", flush=True)

print(f"\nRESUMO: {ok} OK, {falha} falhas em {time.time()-ts_inicio:.0f}s", flush=True)

cur_l.close()
conn_l.close()