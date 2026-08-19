"""Baixar dados TRF6 do Supabase, processar local e fazer upload para o bucket cnefe-data."""
import psycopg2, json, time, gzip, io
from urllib.request import Request, urlopen
from urllib.error import HTTPError

SUPABASE_URL = "https://weaqkaaqalvpbxkxrfee.supabase.co"
SUPABASE_ANON_KEY = "sb_publishable_K1A9Oo6uXRAmIXDATMThEw_jDy1fLAJ"
BUCKET_NAME = "cnefe-data"
ESTADO = "MG"
TRIBUNAL = "TRF6"

# 1. Baixar dados de comarcas TRF6 do Supabase
print("1. Baixando dados TRF6 do Supabase...", flush=True)
conn_s = psycopg2.connect(host='aws-0-us-west-2.pooler.supabase.com', port=5432, dbname='postgres',
    user='postgres.weaqkaaqalvpbxkxrfee', password='Senha14498620@', connect_timeout=30)
cur_s = conn_s.cursor()

# Subseções TRF6
cur_s.execute("SELECT codigo, nome FROM cnj_comarcas WHERE tribunal_codigo = '06' AND ativo = true ORDER BY codigo")
subsecoes = cur_s.fetchall()
print(f"   {len(subsecoes)} subseções TRF6")

# Cidades por subseção
cur_s.execute("SELECT comarca_oooo, cidade FROM cnj_comarcas_cidades WHERE tribunal_codigo = '06' ORDER BY comarca_oooo, cidade")
cidades_subsecao = {}
for r in cur_s.fetchall():
    sigla = r[0]
    cidade = r[1].upper()
    cidades_subsecao.setdefault(sigla, []).append(cidade)

cur_s.close()
conn_s.close()

# 2. Conectar ao banco local
print("2. Processando localmente...", flush=True)
conn_l = psycopg2.connect(host='localhost', port=5433, user='postgres', password='postgres', dbname='oficiojus_drive')
cur_l = conn_l.cursor()

def upload(gzipped, sigla, nome):
    path = f"{ESTADO}/{TRIBUNAL}/{sigla}/cnefe_unificada.ndjson.gz"
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

for idx, (sigla, nome) in enumerate(subsecoes, 1):
    cidades = cidades_subsecao.get(sigla, [])
    if not cidades:
        print(f"[{idx:2d}/{len(subsecoes)}] {sigla} {nome}: SEM CIDADES", flush=True)
        falha += 1
        continue

    ts = time.time()
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
        print(f"[{idx:2d}/{len(subsecoes)}] {sigla} {nome}: 0 registros", flush=True)
        falha += 1
        continue

    sucesso = upload(gzipped, sigla, nome)
    if sucesso:
        ok += 1
        status = "OK  ✅"
    else:
        falha += 1
        status = "ERR ❌"

    print(f"[{idx:2d}/{len(subsecoes)}] {sigla} {nome}: {count:>6,} reg, {tamanho_mb:>5.1f} MB, {tempo:>3.0f}s  {status}", flush=True)

print(f"\nRESUMO TRF6: {ok} OK, {falha} falhas em {time.time()-ts_inicio:.0f}s", flush=True)

cur_l.close()
conn_l.close()