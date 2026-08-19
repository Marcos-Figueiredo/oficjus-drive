"""Refazer upload da comarca GUANHÃES (0280) que falhou."""
import psycopg2, json, time, gzip, io
from urllib.request import Request, urlopen
from urllib.error import HTTPError

SUPABASE_URL = "https://weaqkaaqalvpbxkxrfee.supabase.co"
SUPABASE_ANON_KEY = "sb_publishable_K1A9Oo6uXRAmIXDATMThEw_jDy1fLAJ"
BUCKET_NAME = "cnefe-data"

conn = psycopg2.connect(host='localhost', port=5433, user='postgres', password='postgres', dbname='oficiojus_drive')
cur = conn.cursor()

comarca_id = "0280"
cur.execute("SELECT cidade FROM cnj_comarcas_cidades WHERE comarca_oooo = %s", (comarca_id,))
cidades = [r[0].upper() for r in cur.fetchall()]
print(f"Comarca 0280 GUANHÃES: {len(cidades)} cidades")

placeholders = ','.join(['%s'] * len(cidades))
cur.execute(f"""
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
print(f"Gerado: {count} registros, {len(gzipped)/1024/1024:.1f} MB")

# Upload com retry
path = f"MG/TJMG/{comarca_id}/cnefe_unificada.ndjson.gz"
for tentativa in range(1, 4):
    try:
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
        urlopen(req)
        print(f"Upload OK (tentativa {tentativa})!")
        break
    except HTTPError as e:
        print(f"Tentativa {tentativa}: HTTP {e.code} - {e.read().decode()[:100]}")
        if tentativa < 3:
            time.sleep(3)
else:
    print("FALHOU todas as tentativas")

cur.close()
conn.close()