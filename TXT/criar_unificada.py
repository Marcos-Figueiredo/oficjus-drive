"""Script definitivo: cria cnefe_unificada local, exporta NDJSON.gz, envia ao Supabase."""
import psycopg2, json, time, gzip, os, io
from urllib.request import Request, urlopen
from urllib.error import HTTPError, URLError

# === CONFIG ===
SUPABASE_URL = "https://weaqkaaqalvpbxkxrfee.supabase.co"
SUPABASE_ANON_KEY = "sb_publishable_K1A9Oo6uXRAmIXDATMThEw_jDy1fLAJ"
# Para upload no Storage precisamos de service_role (operação administrativa)
# Se não tivermos a chave, faremos via anon key se o bucket for público
# Tentaremos primeiro sem auth, depois com anon

OUTPUT_DIR = "c:\\oficjus-drive\\build"
OUTPUT_FILE = "cnefe_unificada.ndjson.gz"
OUTPUT_PATH = os.path.join(OUTPUT_DIR, OUTPUT_FILE)
os.makedirs(OUTPUT_DIR, exist_ok=True)

conn = psycopg2.connect(host='localhost', port=5433, user='postgres', password='postgres', dbname='oficiojus_drive')
cur = conn.cursor()

print("=" * 60)
print("1. CRIANDO cnefe_unificada DEFINITIVA")
print("=" * 60)
ts = time.time()

cur.execute("DROP TABLE IF EXISTS cnefe_unificada")
cur.execute("""
    CREATE TABLE cnefe_unificada AS
    SELECT 
        l.id,
        l.logradouro_completo,
        l.bairro,
        l.cep,
        l.cidade,
        l.estado,
        l.geometry,
        l.extensao_metros,
        l.menor_numero,
        l.maior_numero,
        '{}'::jsonb AS numeros_por_cep
    FROM cnefe_logradouros l
""")  # SEM WHERE - inclui TODOS, inclusive os 88k sem geometry
conn.commit()

cur.execute("SELECT COUNT(*) FROM cnefe_unificada")
total_log = cur.fetchone()[0]
cur.execute("SELECT COUNT(*) FROM cnefe_unificada WHERE geometry IS NOT NULL")
com_geo = cur.fetchone()[0]
cur.execute("SELECT COUNT(*) FROM cnefe_unificada WHERE geometry IS NULL")
sem_geo = cur.fetchone()[0]
print(f"  Total: {total_log:,} registros ({com_geo:,} com geo, {sem_geo:,} sem geo)")
print(f"  Criada em {time.time()-ts:.0f}s", flush=True)

print(f"\n2. POPULANDO numeros_por_cep (BULK)")
print("=" * 60)
ts = time.time()

# Temp table com todos os numeros agrupados
cur.execute("DROP TABLE IF EXISTS temp_numeros_agg")
cur.execute("""
    CREATE TEMP TABLE temp_numeros_agg AS
    SELECT n.logradouro_id, n.cep, 
           jsonb_agg(jsonb_build_array(n.numero::int, n.latitude, n.longitude) ORDER BY n.numero::int) AS numeros
    FROM cnefe_numeros n
    WHERE n.latitude IS NOT NULL AND n.longitude IS NOT NULL
      AND n.numero ~ '^[0-9]+$'
    GROUP BY n.logradouro_id, n.cep
""")
conn.commit()

cur.execute("SELECT COUNT(*) FROM temp_numeros_agg")
total_grupos = cur.fetchone()[0]
print(f"  {total_grupos:,} grupos (logradouro, cep)", flush=True)

cur.execute("""
    UPDATE cnefe_unificada u
    SET numeros_por_cep = agg.numeros_por_cep
    FROM (
        SELECT logradouro_id, 
               jsonb_object_agg(cep, numeros) AS numeros_por_cep
        FROM temp_numeros_agg
        GROUP BY logradouro_id
    ) agg
    WHERE u.id = agg.logradouro_id
""")
conn.commit()
atualizados = cur.rowcount
print(f"  {atualizados:,} logradouros atualizados em {time.time()-ts:.0f}s", flush=True)

# Limpar temp
cur.execute("DROP TABLE IF EXISTS temp_numeros_agg")
conn.commit()

# Estatisticas finais
print(f"\n3. ESTATISTICAS DA TABELA FINAL")
print("=" * 60)
cur.execute("SELECT pg_total_relation_size('cnefe_unificada') as total_bytes")
r = cur.fetchone()
print(f"  Tamanho no PostgreSQL: {r[0]/1024/1024:.1f} MB")

cur.execute("""
    SELECT SUM(pg_column_size(numeros_por_cep)) as jsonb_bytes,
           COUNT(*) FILTER (WHERE geometry IS NOT NULL) as com_geo,
           COUNT(*) FILTER (WHERE geometry IS NULL) as sem_geo,
           COUNT(*) FILTER (WHERE numeros_por_cep = '{}'::jsonb) as sem_numeros,
           COUNT(*) FILTER (WHERE numeros_por_cep != '{}'::jsonb) as com_numeros
    FROM cnefe_unificada
""")
r2 = cur.fetchone()
print(f"  Tamanho do JSONB: {(r2[0] or 0)/1024/1024:.1f} MB")
print(f"  Com geometry: {r2[1]:,} | Sem geometry: {r2[2]:,}")
print(f"  Com numeros_por_cep: {r2[4]:,} | Sem numeros: {r2[3]:,}")

print(f"\n4. EXPORTANDO NDJSON.GZ (SEM GEOMETRY)")
print("=" * 60)
ts = time.time()

cur.execute("""
    SELECT id, logradouro_completo, bairro, cep, cidade, estado,
           extensao_metros, menor_numero, maior_numero,
           CASE WHEN geometry IS NULL THEN true ELSE false END as sem_geo,
           numeros_por_cep
    FROM cnefe_unificada
    ORDER BY id
""")

buf = io.StringIO()
count = 0
for row in cur.fetchall():
    rec = {
        "i": row[0],          # id (int)
        "l": row[1],          # logradouro (string)
        "b": row[2],          # bairro (string)
        "c": row[3],          # cep (string)
        "cid": row[4],        # cidade (string)
        "e": row[5],          # estado (string)
        "ext": row[6],        # extensao_metros (float)
        "mn": row[7],         # menor_numero (int)
        "mx": row[8],         # maior_numero (int)
        "sg": row[9],         # sem_geo (bool)
        "n": row[10]          # numeros_por_cep (dict)
    }
    # Só inclui geometry se existir (para manter compatibilidade
    # com quem quiser usar no futuro)
    buf.write(json.dumps(rec, ensure_ascii=False))
    buf.write('\n')
    count += 1
    if count % 50000 == 0:
        print(f"  {count:,} registros processados...", flush=True)

raw = buf.getvalue()
buf.close()
raw_bytes = len(raw.encode('utf-8'))

# Comprimir
gzipped = gzip.compress(raw.encode('utf-8'), compresslevel=6)

# Salvar
with open(OUTPUT_PATH, 'wb') as f:
    f.write(gzipped)

print(f"  NDJSON cru:            {raw_bytes/1024/1024:.1f} MB")
print(f"  NDJSON.gz (level 6):   {len(gzipped)/1024/1024:.1f} MB")
print(f"  Taxa compressao:       {raw_bytes/len(gzipped):.1f}x")
print(f"  Exportado em {time.time()-ts:.0f}s", flush=True)
print(f"  Arquivo: {OUTPUT_PATH}")

# 5. Upload para Supabase Storage
print(f"\n5. UPLOAD PARA SUPABASE STORAGE")
print("=" * 60)
ts = time.time()

bucket_name = "cnefe-data"  # precisamos criar o bucket
storage_path = f"mg/{OUTPUT_FILE}"

# Tentar upload via API REST do Supabase Storage
# Primeiro verifica se o bucket existe, se não, tenta criar
headers = {
    "apikey": SUPABASE_ANON_KEY,
    "Authorization": f"Bearer {SUPABASE_ANON_KEY}",
    "Content-Type": "application/json"
}

# Verificar/Listar buckets
req = Request(
    f"{SUPABASE_URL}/storage/v1/bucket",
    headers=headers,
    method="GET"
)
try:
    resp = urlopen(req)
    buckets = json.loads(resp.read())
    bucket_names = [b["name"] for b in buckets]
    print(f"  Buckets existentes: {', '.join(bucket_names)}")
    
    if bucket_name not in bucket_names:
        print(f"  Bucket '{bucket_name}' nao existe. Criando...")
        req = Request(
            f"{SUPABASE_URL}/storage/v1/bucket",
            data=json.dumps({
                "id": bucket_name,
                "name": bucket_name,
                "public": True,
                "file_size_limit": 104857600  # 100MB
            }).encode(),
            headers={**headers, "Content-Type": "application/json"},
            method="POST"
        )
        resp = urlopen(req)
        print(f"  Bucket criado: {json.loads(resp.read())}")
    else:
        print(f"  Bucket '{bucket_name}' ja existe")
    
    # Upload do arquivo
    print(f"  Enviando {OUTPUT_FILE} para {storage_path}...", flush=True)
    
    # Usar multipart/form-data via requests-like
    boundary = "----FormBoundary7MA4YWxkTrZu0gW"
    body = (
        f"--{boundary}\r\n"
        f"Content-Disposition: form-data; name=\"file\"; filename=\"{OUTPUT_FILE}\"\r\n"
        f"Content-Type: application/gzip\r\n\r\n"
    ).encode('utf-8') + gzipped + f"\r\n--{boundary}--\r\n".encode('utf-8')
    
    req = Request(
        f"{SUPABASE_URL}/storage/v1/object/{bucket_name}/{storage_path}",
        data=body,
        headers={
            "apikey": SUPABASE_ANON_KEY,
            "Authorization": f"Bearer {SUPABASE_ANON_KEY}",
            "Content-Type": f"multipart/form-data; boundary={boundary}"
        },
        method="POST"
    )
    resp = urlopen(req)
    result = json.loads(resp.read())
    print(f"  Upload concluido: {result}")
    public_url = f"{SUPABASE_URL}/storage/v1/object/public/{bucket_name}/{storage_path}"
    print(f"  URL publica: {public_url}")
    
except HTTPError as e:
    error_body = e.read().decode()
    print(f"  ERRO HTTP {e.code}: {error_body[:200]}")
    print(f"\n  === Upload falhou (provavelmente precisa de service_role key) ===")
    print(f"  O arquivo foi salvo localmente em: {OUTPUT_PATH}")
    print(f"  Para fazer upload manualmente:")
    print(f"    1. Crie um bucket 'cnefe-data' (público) no Supabase Dashboard")
    print(f"    2. Faça upload de: {OUTPUT_PATH}")
    print(f"    3. Destino: {storage_path}")
except URLError as e:
    print(f"  ERRO DE REDE: {e.reason}")
    print(f"  O arquivo foi salvo localmente em: {OUTPUT_PATH}")

cur.close()
conn.close()
print(f"\n{'='*60}")
print(f"CONCLUIDO! ({time.time()-ts:.0f}s)")