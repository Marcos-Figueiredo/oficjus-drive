"""Gerar NDJSON.gz para TODAS as comarcas TJMG + TRF6 com quadro de evolução em tempo real."""
import psycopg2, json, time, gzip, io, sys, os
from urllib.request import Request, urlopen
from urllib.error import HTTPError

SUPABASE_URL = "https://weaqkaaqalvpbxkxrfee.supabase.co"
SUPABASE_ANON_KEY = "sb_publishable_K1A9Oo6uXRAmIXDATMThEw_jDy1fLAJ"
BUCKET_NAME = "cnefe-data"

# ============================================================
# QUADRO DE EVOLUÇÃO — atualizado a cada comarca processada
# ============================================================
LOG_FILE = r'c:\oficjus-drive\TXT\evolucao_exportacao.txt'

def limpar_quadro():
    with open(LOG_FILE, 'w', encoding='utf-8') as f:
        f.write("")

def escrever_quadro(linhas, cabecalho=None):
    """Escreve o quadro de evolução no arquivo."""
    with open(LOG_FILE, 'w', encoding='utf-8') as f:
        if cabecalho:
            f.write(cabecalho + '\n')
            f.write('=' * 80 + '\n')
        for linha in linhas:
            f.write(linha + '\n')

def barra(fracao, largura=30):
    """Desenha barra de progresso: [██████░░░░] 60%"""
    preenchido = int(fracao * largura)
    bar = '█' * preenchido + '░' * (largura - preenchido)
    return f"[{bar}] {fracao*100:.0f}%"

# ============================================================
# DADOS
# ============================================================
conn = psycopg2.connect(host='localhost', port=5433, user='postgres', password='postgres', dbname='oficiojus_drive')
cur = conn.cursor()

# Carregar comarcas TJMG
comarcas_tjmg = json.load(open(r'c:\oficjus-drive\TXT\comarcas-tjmg-oficial.json', encoding='utf-8'))
territorios_tjmg = json.load(open(r'c:\oficjus-drive\TXT\territorios-tjmg-oficial.json', encoding='utf-8'))

# Só comarcas ativas
comarcas_ativas = [c for c in comarcas_tjmg if c.get('ativo')]
nomes_comarcas = {c['codigo']: c['nome'] for c in comarcas_ativas}

def upload(comarca_id, gzipped, tribunal):
    path = f"MG/{tribunal}/{comarca_id}/cnefe_unificada.ndjson.gz"
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
                return False
        return False

def processar_comarca(tribunal, codigo, nome, cidades):
    """Processa uma comarca: consulta local, gera NDJSON.gz, upload."""
    ts = time.time()
    cidades_tuple = tuple(cidades)
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
    sucesso = upload(codigo, gzipped, tribunal)
    tempo = time.time() - ts
    return count, tamanho_mb, sucesso, tempo

# ============================================================
# EXECUÇÃO TJMG
# ============================================================
limpar_quadro()
ts_inicio = time.time()

todas_linhas = []
ok_total = 0
falha_total = 0

print("INICIANDO EXPORTAÇÃO TJMG...", flush=True)

tjmg = COMARCAS["TJMG"]
total_comarcas = len(tjmg["codigos"])

for idx, cod in enumerate(tjmg["codigos"], 1):
    nome = tjmg["nomes"].get(cod, cod)
    cidades = [
        t['nome'].upper() for t in tjmg["territorios"]
        if t.get('comarca_codigo') == cod and t.get('tipo') in ('municipio', 'sede')
    ]
    if not cidades:
        todas_linhas.append(f"  [{cod}] {nome}: SEM CIDADES")
        falha_total += 1
        continue

    count, tamanho_mb, sucesso, tempo = processar_comarca("TJMG", cod, nome, cidades)

    if sucesso:
        status = "OK  ✅"
        ok_total += 1
    else:
        status = "ERR ❌"
        falha_total += 1

    todas_linhas.append(f"  [{cod}] {nome}: {count:>6,} registros, {tamanho_mb:>6.1f} MB, {tempo:>4.0f}s  {status}")

    # Escreve o quadro de evolução
    cabecalho = (f"EXPORTAÇÃO COMARCAS — {time.strftime('%d/%m/%Y %H:%M:%S')}\n"
                 f"Progresso: {barra(idx/total_comarcas)}\n"
                 f"Processadas: {idx}/{total_comarcas} ({ok_total} OK, {falha_total} falhas)")
    escrever_quadro(todas_linhas, cabecalho)

    print(f"[{idx}/{total_comarcas}] {nome}: {count:,} reg, {tamanho_mb:.1f} MB, {status}", flush=True)

print(f"\nTJMG CONCLUÍDO: {ok_total} OK, {falha_total} falhas", flush=True)
print(f"Tempo TJMG: {time.time()-ts_inicio:.0f}s", flush=True)

# ============================================================
# EXECUÇÃO TRF6
# ============================================================
print("\nINICIANDO EXPORTAÇÃO TRF6...", flush=True)
ts_trf6 = time.time()

# Carregar subseções TRF6
trf6_json = r'c:\oficjus-drive\TXT\trf6_subsecoes.json'
trf6_nomes = {}

# TRF6 subseções (se arquivo existir)
if os.path.exists(trf6_json):
    with open(trf6_json, encoding='utf-8') as f:
        trf6_data = json.load(f)
else:
    trf6_data = []

trf6_codigos = [s.get('codigo') or s.get('id') or s.get('subsecao') for s in trf6_data]
if not trf6_codigos:
    # Fallback: buscar no banco local
    cur.execute("SELECT DISTINCT subsecao FROM cnefe_unificada WHERE estado = 'MG'")
    trf6_codigos = [r[0] for r in cur.fetchall()]

print(f"  {len(trf6_codigos)} subseções TRF6 encontradas", flush=True)

trf6_linhas = []
for idx, cod in enumerate(trf6_codigos, 1):
    nome = trf6_nomes.get(str(cod), str(cod))
    # Cidades da subseção TRF6 — buscar no banco
    cur.execute("""
        SELECT DISTINCT cidade FROM cnefe_unificada 
        WHERE estado = 'MG'
    """)
    cidades = [r[0] for r in cur.fetchall()]

    count, tamanho_mb, sucesso, tempo = processar_comarca("TRF6", str(cod), nome, cidades)

    if sucesso:
        status = "OK  ✅"
        ok_total += 1
    else:
        status = "ERR ❌"
        falha_total += 1

    trf6_linhas.append(f"  [{cod}] {nome}: {count:>6,} registros, {tamanho_mb:>6.1f} MB, {tempo:>4.0f}s  {status}")

    cabecalho = (f"EXPORTAÇÃO TRF6 — {time.strftime('%d/%m/%Y %H:%M:%S')}\n"
                 f"Progresso: {barra(idx/len(trf6_codigos))}\n"
                 f"Processadas: {idx}/{len(trf6_codigos)}")
    escrever_quadro(trf6_linhas, cabecalho)

    print(f"[{idx}/{len(trf6_codigos)}] {nome}: {count:,} reg, {tamanho_mb:.1f} MB, {status}", flush=True)

print(f"\nTRF6 CONCLUÍDO em {time.time()-ts_trf6:.0f}s", flush=True)
print(f"TOTAL GERAL: {ok_total} OK, {falha_total} falhas", flush=True)

cur.close()
conn.close()