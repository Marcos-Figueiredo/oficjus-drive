"""Gerar NDJSON.gz para TODAS as comarcas TJMG e fazer upload ao Supabase.
Processamento no PostgreSQL local, depois upload dos arquivos finais."""
import psycopg2, json, time, gzip, io, sys
from urllib.request import Request, urlopen
from urllib.error import HTTPError

SUPABASE_URL = "https://weaqkaaqalvpbxkxrfee.supabase.co"
SUPABASE_ANON_KEY = "sb_publishable_K1A9Oo6uXRAmIXDATMThEw_jDy1fLAJ"
BUCKET_NAME = "cnefe-data"
TRIBUNAL = "TJMG"
ESTADO = "MG"

# Comarcas ativas do TJMG
comarcas = [c for c in json.load(open(r'c:\oficjus-drive\TXT\comarcas-tjmg-oficial.json', encoding='utf-8')) if c.get('ativo')]
print(f"{len(comarcas)} comarcas ativas")

conn = psycopg2.connect(host='localhost', port=5433, user='postgres', password='postgres', dbname='oficiojus_drive')
cur = conn.cursor()

def upload(comarca_id, gzipped):
    path = f"{ESTADO}/{TRIBUNAL}/{comarca_id}/cnefe_unificada.ndjson.gz"
    boundary = "----FormBoundary7MA4YWxkTrZu0gW"
    body = (
        f"--{boundary}\r\n"
        f"Content-Disposition: form-data; name=\"file\"; filename=\"cnefe_unificada.ndjson.gz\"\r\n"
        f"Content-Type: application/gzip\r\n\r\n"
    ).encode('utf-8') + gzipped + f"\r\n--{boundary}--\r\n".encode('utf-8')
    req = Request(
        f"{SUPABASE_URL}/storage/v1/object/{BUCKET_NAME}/{path}",
        data=body,
        headers={
            "apikey": SUPABASE_ANON_KEY,
            "Authorization": f"Bearer {SUPABASE_ANON_KEY}",
            "Content-Type": f"multipart/form-data; boundary={boundary}"
        },
        method="POST"
    )
    try:
        urlopen(req)
        return True
    except HTTPError as e:
        if e.code == 413:
            # Tenta PUT
            req2 = Request(
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
                urlopen(req2)
                return True
            except HTTPError as e2:
                print(f"    PUT ERRO {e2.code}: {e2.read().decode()[:100]}")
                return False
        print(f"    ERRO {e.code}: {e.read().decode()[:100]}")
        return False

# Carregar todos os territórios para mapear comarca → cidades
territorios = json.load(open(r'c:\oficjus-drive\TXT\territorios-tjmg-oficial.json', encoding='utf-8'))

ok = 0
falhou = 0
total_registros = 0
total_tamanho = 0
ts_inicio = time.time()

for comarca in comarcas:
    cod = comarca['codigo']
    nome = comarca['nome']

    # Cidades da comarca (municípios + sede)
    cidades = [
        t['nome'].upper() for t in territorios
        if t.get('comarca_codigo') == cod and t.get('tipo') in ('municipio', 'sede')
    ]
    if not cidades:
        print(f"  [{cod}] {nome}: SEM CIDADES, pulando")
        falhou += 1
        continue

    # Consulta no banco local
    cidades_tuple = tuple(cidades)
    ts = time.time()
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
    gzipped = gzip.compress(raw.encode('utf-8'), compresslevel=6)
    tamanho_mb = len(gzipped) / 1024 / 1024

    # Upload
    if upload(cod, gzipped):
        ok += 1
        print(f"  [{cod}] {nome}: {count:,} registros, {tamanho_mb:.1f} MB - OK ({time.time()-ts:.0f}s)")
    else:
        falhou += 1
        print(f"  [{cod}] {nome}: {count:,} registros - FALHOU UPLOAD")

    total_registros += count
    total_tamanho += tamanho_mb

print(f"\n{'='*60}")
print(f"RESUMO: {ok} comarcas OK, {falhou} falhas")
print(f"Total: {total_registros:,} registros, {total_tamanho:.1f} MB")
print(f"Tempo total: {time.time()-ts_inicio:.0f}s")

cur.close()
conn.close()