"""Gerar NDJSON.gz por comarca (cidades) e fazer upload pro Supabase Storage.
Regra: processamento no PostgreSQL local, depois upload do arquivo final."""
import psycopg2, json, time, gzip, io, os, sys
from urllib.request import Request, urlopen
from urllib.error import HTTPError

SUPABASE_URL = "https://weaqkaaqalvpbxkxrfee.supabase.co"
SUPABASE_ANON_KEY = "sb_publishable_K1A9Oo6uXRAmIXDATMThEw_jDy1fLAJ"
BUCKET_NAME = "cnefe-data"

conn = psycopg2.connect(host='localhost', port=5433, user='postgres', password='postgres', dbname='oficiojus_drive')
cur = conn.cursor()

# Comarca 0672 = Sete Lagoas
COMARCA_ID = "0672"
CIDADES = [
    "BALDIM", "CACHOEIRA DA PRATA", "FORTUNA DE MINAS", "FUNILÂNDIA",
    "INHAÚMA", "JEQUITIBÁ", "SANTANA DE PIRAPAMA", "SETE LAGOAS"
]
TRIBUNAL = "TJMG"
ESTADO = "MG"

storage_path = f"{ESTADO}/{TRIBUNAL}/{COMARCA_ID}/cnefe_unificada.ndjson.gz"

print(f"1. EXPORTANDO NDJSON (COMARCA {COMARCA_ID} - {', '.join(CIDADES)})", flush=True)
ts = time.time()

# Converte lista de cidades para formato SQL
cidades_tuple = tuple(CIDADES)
cur.execute(f"""
    SELECT id, logradouro_completo, bairro, cep, cidade, estado,
           extensao_metros, menor_numero, maior_numero,
           CASE WHEN geometry IS NULL THEN true ELSE false END as sem_geo,
           numeros_por_cep
    FROM cnefe_unificada
    WHERE cidade IN {cidades_tuple}
    ORDER BY id
""")

buf = io.StringIO()
count = 0
for row in cur.fetchall():
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
raw_bytes = len(raw.encode('utf-8'))
gzipped = gzip.compress(raw.encode('utf-8'), compresslevel=6)

print(f"  {count:,} registros exportados em {time.time()-ts:.0f}s", flush=True)
print(f"  NDJSON.gz: {len(gzipped)/1024/1024:.1f} MB", flush=True)

# 2. Upload para Supabase Storage
print(f"\n2. FAZENDO UPLOAD PARA {BUCKET_NAME}/{storage_path}", flush=True)
ts = time.time()

# Cria pasta via upload do arquivo
boundary = "----FormBoundary7MA4YWxkTrZu0gW"
body = (
    f"--{boundary}\r\n"
    f"Content-Disposition: form-data; name=\"file\"; filename=\"cnefe_unificada.ndjson.gz\"\r\n"
    f"Content-Type: application/gzip\r\n\r\n"
).encode('utf-8') + gzipped + f"\r\n--{boundary}--\r\n".encode('utf-8')

req = Request(
    f"{SUPABASE_URL}/storage/v1/object/{BUCKET_NAME}/{storage_path}",
    data=body,
    headers={
        "apikey": SUPABASE_ANON_KEY,
        "Authorization": f"Bearer {SUPABASE_ANON_KEY}",
        "Content-Type": f"multipart/form-data; boundary={boundary}"
    },
    method="POST"
)
try:
    resp = urlopen(req)
    result = json.loads(resp.read())
    print(f"  Upload concluido: {result}", flush=True)
    print(f"  URL: {SUPABASE_URL}/storage/v1/object/public/{BUCKET_NAME}/{storage_path}", flush=True)
except HTTPError as e:
    erro = e.read().decode()
    print(f"  ERRO HTTP {e.code}: {erro[:300]}", flush=True)
    # Se for 413 (muito grande), tenta com PUT
    if e.code == 413:
        print("  Tentando PUT...", flush=True)
        req = Request(
            f"{SUPABASE_URL}/storage/v1/object/{BUCKET_NAME}/{storage_path}",
            data=gzipped,
            headers={
                "apikey": SUPABASE_ANON_KEY,
                "Authorization": f"Bearer {SUPABASE_ANON_KEY}",
                "Content-Type": "application/gzip"
            },
            method="PUT"
        )
        try:
            resp = urlopen(req)
            print(f"  Upload PUT concluido!", flush=True)
        except HTTPError as e2:
            print(f"  ERRO PUT {e2.code}: {e2.read().decode()[:200]}", flush=True)

print(f"\nCONCLUIDO em {time.time()-ts:.0f}s!", flush=True)
cur.close()
conn.close()