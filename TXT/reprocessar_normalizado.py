"""Corrigir mapeamento de cidades para o padrão CNEFE e reprocessar comarcas com normalização."""
import psycopg2, unicodedata, re, json, time, gzip, io

def normalizar(texto):
    """Remove acentos, hífens, etc. (padrão CNEFE/IBGE)."""
    t = unicodedata.normalize('NFD', texto)
    t = ''.join(c for c in t if unicodedata.category(c) != 'Mn')
    t = t.upper()
    t = re.sub(r'[^A-Z0-9 ]', ' ', t)
    t = re.sub(r'\s+', ' ', t).strip()
    return t

# Mapeamento manual de correções (casos que normalização não resolve)
CORRECOES = {
    "ITAMOJI": "ITAMOGI",  # IBGE escreve com G, TJMG com J
}

conn = psycopg2.connect(host='localhost', port=5433, user='postgres', password='postgres', dbname='oficiojus_drive')
cur = conn.cursor()

# 1. Índice de cidades CNEFE por nome normalizado
cur.execute("SELECT DISTINCT cidade FROM cnefe_unificada")
cidades_cnefe = {r[0] for r in cur.fetchall()}
norm_cnefe = {}
for c in cidades_cnefe:
    norm_cnefe.setdefault(normalizar(c), c)

# 2. Buscar todas as comarcas com cidades
cur.execute("""
    SELECT DISTINCT c.comarca_oooo, c.cidade
    FROM cnj_comarcas_cidades c
    JOIN cnj_comarcas k ON k.codigo = c.comarca_oooo AND k.tribunal_codigo = '13'
    WHERE c.tribunal_codigo = '13'
""")
mapeamento = cur.fetchall()

from collections import defaultdict
comarca_uf_cidades = defaultdict(set)

for comarca, cidade in mapeamento:
    # Tenta match direto
    cidade_upper = cidade.upper()
    if cidade_upper in cidades_cnefe:
        comarca_uf_cidades[comarca].add(cidade_upper)
        continue
    # Tenta normalização
    norm = normalizar(cidade_upper)
    if norm in norm_cnefe:
        comarca_uf_cidades[comarca].add(norm_cnefe[norm])
        continue
    # Tenta correção manual
    if cidade_upper in CORRECOES:
        corr = CORRECOES[cidade_upper]
        if corr in cidades_cnefe:
            comarca_uf_cidades[comarca].add(corr)
            print(f"  Correção manual: {cidade} -> {corr}")
            continue

# 3. Reprocessar comarcas que ficaram sem dados
print("\nReprocessando comarcas que antes estavam sem dados...", flush=True)

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

# Lista de comarcas que ficaram sem dados (ver resultado do mapear_normalizacao.py)
# 0329 ITAMOJI precisa de correção manual, 0476 PASSA-QUATRO agora resolve com normalização
comarcas_reprocessar = ['0329', '0476', '0477', '0003', '0073', '0134', '0183', '0239',
                        '0278', '0486', '0570', '0625', '0686']

ok = 0
falha = 0
for comarca_id in comarcas_reprocessar:
    cidades = comarca_uf_cidades.get(comarca_id, set())
    if not cidades:
        # Pega cidades originais para debug
        cur.execute("SELECT cidade FROM cnj_comarcas_cidades WHERE comarca_oooo = %s", (comarca_id,))
        orig = [r[0] for r in cur.fetchall()]
        print(f"  [{comarca_id}] {orig}: SEM CIDADES CNEFE (pulando)", flush=True)
        falha += 1
        continue

    ts = time.time()
    cidades_list = list(cidades)
    placeholders = ','.join(['%s'] * len(cidades_list))
    cur.execute(f"""
        SELECT id, logradouro_completo, bairro, cep, cidade, estado,
               extensao_metros, menor_numero, maior_numero,
               CASE WHEN geometry IS NULL THEN true ELSE false END as sem_geo,
               numeros_por_cep
        FROM cnefe_unificada
        WHERE cidade IN ({placeholders})
        ORDER BY id
    """, cidades_list)

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
    tempo = time.time() - ts

    if count == 0:
        print(f"  [{comarca_id}]: 0 registros (pulando)", flush=True)
        falha += 1
        continue

    sucesso = upload(gzipped, comarca_id)
    if sucesso:
        ok += 1
        print(f"  [{comarca_id}] {cidades_list}: {count} reg, {tamanho_mb:.1f} MB, {tempo:.0f}s  OK", flush=True)
    else:
        falha += 1
        print(f"  [{comarca_id}] {cidades_list}: FALHA UPLOAD", flush=True)

print(f"\nRESUMO: {ok} OK, {falha} falhas", flush=True)

cur.close()
conn.close()