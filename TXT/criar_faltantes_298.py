"""Criar as 3 comarcas faltantes e gerar NDJSONs."""
import psycopg2, json, time, gzip, io, unicodedata, re
from urllib.request import Request, urlopen
from urllib.error import HTTPError

SUPABASE_URL = "https://weaqkaaqalvpbxkxrfee.supabase.co"
SUPABASE_ANON_KEY = "sb_publishable_K1A9Oo6uXRAmIXDATMThEw_jDy1fLAJ"
BUCKET_NAME = "cnefe-data"

conn_s = psycopg2.connect(host='aws-0-us-west-2.pooler.supabase.com', port=5432, dbname='postgres',
    user='postgres.weaqkaaqalvpbxkxrfee', password='Senha14498620@', connect_timeout=30)
cur_s = conn_s.cursor()

conn_l = psycopg2.connect(host='localhost', port=5433, user='postgres', password='postgres', dbname='oficiojus_drive')
cur_l = conn_l.cursor()

# 3 comarcas para criar
faltantes = {
    "0627": {"nome": "São João do Paraíso"},
    "0738": {"nome": "Jaíba"},
    "0740": {"nome": "Juatuba"},
}

def normalizar(texto):
    t = unicodedata.normalize('NFD', texto)
    t = ''.join(c for c in t if unicodedata.category(c) != 'Mn')
    t = t.upper()
    t = re.sub(r'[^A-Z0-9 ]', ' ', t).strip()
    return t

# 1. Criar comarcas no Supabase
print("1. Criando comarcas no Supabase...", flush=True)
for cod, info in faltantes.items():
    # Get next available id
    cur_s.execute("SELECT MAX(id) FROM cnj_comarcas")
    max_id = cur_s.fetchone()[0] or 0
    next_id = max_id + 1

    cur_s.execute(
        "INSERT INTO cnj_comarcas (id, segmento_codigo, tribunal_codigo, codigo, nome, ordem, ativo) VALUES (%s, '8', '13', %s, %s, %s, true)",
        (next_id, cod, info['nome'], 999)
    )
    print(f"   {cod} {info['nome']}: criada (id={next_id})")

    # Adicionar cidade (mesmo nome da comarca)
    cur_s.execute("SELECT MAX(id) FROM cnj_comarcas_cidades")
    max_id_cid = cur_s.fetchone()[0] or 0
    cur_s.execute(
        "INSERT INTO cnj_comarcas_cidades (id, segmento_codigo, tribunal_codigo, comarca_oooo, cidade, tipo) VALUES (%s, '8', '13', %s, %s, 'sede')",
        (max_id_cid + 1, cod, info['nome'].upper())
    )
    print(f"      cidade {info['nome'].upper()} adicionada")
    conn_s.commit()

# 2. Gerar NDJSON e upload
print(f"\n2. Gerando NDJSON e upload...", flush=True)

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

todas = list(faltantes.items())
# Adicionar Januária 0252 que precisa de upload
todas.insert(0, ("0252", {"nome": "Januária"}))

for cod, info in todas:
    ts = time.time()
    cidade = info['nome'].upper()
    
    # Tenta match direto
    cur_l.execute("SELECT COUNT(*) FROM cnefe_unificada WHERE cidade = %s", (cidade,))
    total = cur_l.fetchone()[0]
    
    if total == 0:
        # Tenta normalizado
        n = normalizar(cidade)
        cur_l.execute("SELECT cidade FROM cnefe_unificada WHERE cidade ILIKE %s", (f'%{n[:6]}%',))
        found = cur_l.fetchall()
        if found:
            cidade = found[0][0]
            print(f"   {cod} {info['nome']}: normalizado -> {cidade}", flush=True)
        else:
            print(f"   {cod} {info['nome']}: 0 registros no CNEFE, pulando", flush=True)
            continue

    cur_l.execute(f"""
        SELECT id, logradouro_completo, bairro, cep, cidade, estado,
               extensao_metros, menor_numero, maior_numero,
               CASE WHEN geometry IS NULL THEN true ELSE false END as sem_geo,
               numeros_por_cep
        FROM cnefe_unificada
        WHERE cidade = %s
        ORDER BY id
    """, (cidade,))

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
    tempo = time.time() - ts

    if count == 0:
        print(f"   {cod} {info['nome']}: 0 registros", flush=True)
        continue

    sucesso = upload(gzipped, cod)
    status = "OK ✅" if sucesso else "ERR ❌"
    print(f"   {cod} {info['nome']}: {count} reg, {len(gzipped)/1024/1024:.1f} MB, {tempo:.0f}s  {status}", flush=True)

# 3. Verificar total final
cur_s.execute("SELECT COUNT(*) FROM cnj_comarcas WHERE tribunal_codigo = '13'")
total_final = cur_s.fetchone()[0]
print(f"\n3. Total final TJMG no Supabase: {total_final}", flush=True)

cur_s.close()
conn_s.close()
cur_l.close()
conn_l.close()
print(f"Concluído!", flush=True)