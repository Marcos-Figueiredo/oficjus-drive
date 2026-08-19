"""Corrigir comarcas: ajustar Januária, adicionar as 4 faltantes e gerar NDJSONs."""
import psycopg2, json, time, gzip, io
from urllib.request import Request, urlopen
from urllib.error import HTTPError

SUPABASE_URL = "https://weaqkaaqalvpbxkxrfee.supabase.co"
SUPABASE_ANON_KEY = "sb_publishable_K1A9Oo6uXRAmIXDATMThEw_jDy1fLAJ"
BUCKET_NAME = "cnefe-data"

# 1. Conectar Supabase e Local
conn_s = psycopg2.connect(host='aws-0-us-west-2.pooler.supabase.com', port=5432, dbname='postgres',
    user='postgres.weaqkaaqalvpbxkxrfee', password='Senha14498620@', connect_timeout=30)
cur_s = conn_s.cursor()

conn_l = psycopg2.connect(host='localhost', port=5433, user='postgres', password='postgres', dbname='oficiojus_drive')
cur_l = conn_l.cursor()

# 2. Ver o que temos de Januária (0352 - código errado)
print("1. Verificando Januária (0352 -> 0252)...", flush=True)
cur_s.execute("SELECT codigo, nome FROM cnj_comarcas WHERE codigo = '0352'")
print(f"   Supabase 0352: {cur_s.fetchall()}")

cur_s.execute("SELECT comarca_oooo, cidade FROM cnj_comarcas_cidades WHERE comarca_oooo = '0352'")
cidades_0352 = cur_s.fetchall()
print(f"   Cidades 0352: {cidades_0352}")

# 3. Ver no Guia Judiciário / buscar cidades de Januária
# Buscar no original_ibge as cidades com código 0252
cur_l.execute("SELECT DISTINCT cod_municipio, dsc_localidade FROM original_ibge WHERE dsc_localidade = 'JANUÁRIA'")
print(f"\n2. Januária no original_ibge: {cur_l.fetchall()}")

# Verificar se há cidades no CNEFE para Januária
cur_l.execute("SELECT COUNT(*) FROM cnefe_unificada WHERE cidade = 'JANUÁRIA'")
print(f"   Registros JANUÁRIA no CNEFE: {cur_l.fetchone()[0]}")

# 4. Verificar dados das outras comarcas faltantes
faltantes = {
    "0252": {"nome": "Januária", "cidades_orig": ["JANUÁRIA"]},
    "0627": {"nome": "São João do Paraíso", "cidades_orig": ["SÃO JOÃO DO PARAÍSO"]},
    "0738": {"nome": "Jaíba", "cidades_orig": ["JAÍBA"]},
    "0740": {"nome": "Juatuba", "cidades_orig": ["JUATUBA"]},
}

for cod, info in faltantes.items():
    for cid in info["cidades_orig"]:
        cur_l.execute("SELECT COUNT(*) FROM cnefe_unificada WHERE cidade = %s", (cid.upper(),))
        total = cur_l.fetchone()[0]
        print(f"   {cod} {info['nome']} - {cid}: {total} registros no CNEFE")

# 5. CORRIGIR Januária: deletar 0352, inserir 0252
print(f"\n3. Corrigindo Januária no Supabase...", flush=True)

# Copiar cidades de 0352 para 0252
cur_s.execute("UPDATE cnj_comarcas_cidades SET comarca_oooo = '0252' WHERE comarca_oooo = '0352'")
print(f"   Cidades movidas: {cur_s.rowcount}")

# Atualizar código da comarca
cur_s.execute("UPDATE cnj_comarcas SET codigo = '0252' WHERE codigo = '0352'")
print(f"   Comarca atualizada: {cur_s.rowcount}")

conn_s.commit()

# 6. Adicionar as 4 comarcas faltantes
print(f"\n4. Adicionando comarcas faltantes...", flush=True)
for cod, info in faltantes.items():
    # Ver se já existe
    cur_s.execute("SELECT COUNT(*) FROM cnj_comarcas WHERE codigo = %s", (cod,))
    if cur_s.fetchone()[0] == 0:
        cur_s.execute(
            "INSERT INTO cnj_comarcas (segmento_codigo, tribunal_codigo, codigo, nome, ordem, ativo) VALUES (%s, %s, %s, %s, %s, true)",
            ('8', '13', cod, info['nome'], 999)
        )
        print(f"   {cod} {info['nome']}: criada")
        
        # Adicionar cidades
        for cid in info['cidades_orig']:
            cur_s.execute(
                "INSERT INTO cnj_comarcas_cidades (segmento_codigo, tribunal_codigo, comarca_oooo, cidade, tipo) VALUES (%s, %s, %s, %s, 'municipio')",
                ('8', '13', cod, cid.upper())
            )
            print(f"      cidade {cid} adicionada")
    else:
        print(f"   {cod} {info['nome']}: já existe")

conn_s.commit()

# 7. Gerar NDJSON e fazer upload para as 5 comarcas (0252 + 4)
print(f"\n5. Gerando NDJSON e fazendo upload...", flush=True)

def upload(gzipped, comarca_id, tribunal="TJMG"):
    path = f"MG/{tribunal}/{comarca_id}/cnefe_unificada.ndjson.gz"
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
for cod, info in todas:
    ts = time.time()
    cidades = info['cidades_orig']
    # Normalizar nomes (CNEFE)
    cidades_normalizadas = []
    for c in cidades:
        c_up = c.upper()
        # Tenta match direto
        cur_l.execute("SELECT COUNT(*) FROM cnefe_unificada WHERE cidade = %s", (c_up,))
        if cur_l.fetchone()[0] > 0:
            cidades_normalizadas.append(c_up)
        else:
            # Tenta normalizar
            import unicodedata, re
            n = unicodedata.normalize('NFD', c_up)
            n = ''.join(x for x in n if unicodedata.category(x) != 'Mn')
            n = re.sub(r'[^A-Z0-9 ]', ' ', n).strip()
            cur_l.execute("SELECT cidade FROM cnefe_unificada WHERE cidade ILIKE %s", (f'%{n[:6]}%',))
            found = cur_l.fetchall()
            if found:
                cidades_normalizadas.append(found[0][0])
                print(f"   {cod} {info['nome']}: {c_up} -> {found[0][0]} (normalizado)")
            else:
                print(f"   {cod} {info['nome']}: {c_up} -> sem match no CNEFE")

    if not cidades_normalizadas:
        print(f"   {cod} {info['nome']}: 0 registros, pulando", flush=True)
        continue

    placeholders = ','.join(['%s'] * len(cidades_normalizadas))
    cur_l.execute(f"""
        SELECT id, logradouro_completo, bairro, cep, cidade, estado,
               extensao_metros, menor_numero, maior_numero,
               CASE WHEN geometry IS NULL THEN true ELSE false END as sem_geo,
               numeros_por_cep
        FROM cnefe_unificada
        WHERE cidade IN ({placeholders})
        ORDER BY id
    """, cidades_normalizadas)

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
    status = "OK" if sucesso else "ERR"
    print(f"   {cod} {info['nome']}: {count} reg, {len(gzipped)/1024/1024:.1f} MB, {tempo:.0f}s  {status}", flush=True)

cur_s.close()
conn_s.close()
cur_l.close()
conn_l.close()
print(f"\nConcluído!", flush=True)