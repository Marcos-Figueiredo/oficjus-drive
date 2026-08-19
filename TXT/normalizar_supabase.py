"""Normalizar nomes no Supabase para o padrão CNEFE (sem acentos, sem hífens).
Usa UPDATE em massa via VALUES para ser rápido no pooler do Supabase."""
import psycopg2, unicodedata, re

def normalizar(texto):
    """Remove acentos e caracteres especiais (padrão CNEFE). UPPERCASE."""
    t = unicodedata.normalize('NFD', texto)
    t = ''.join(c for c in t if unicodedata.category(c) != 'Mn')
    t = t.upper()
    t = re.sub(r'[^A-Z0-9 ]', ' ', t)
    t = re.sub(r'\s+', ' ', t).strip()
    return t

conn = psycopg2.connect(host='aws-0-us-west-2.pooler.supabase.com', port=5432, dbname='postgres',
    user='postgres.weaqkaaqalvpbxkxrfee', password='Senha14498620@', connect_timeout=30)
cur = conn.cursor()

# ──────────────────────────────────────────────
# 1. Normalizar cnj_comarcas (298 nomes)
# ──────────────────────────────────────────────
print("1. Normalizando cnj_comarcas (nomes)...", flush=True)
cur.execute("SELECT codigo, nome FROM cnj_comarcas WHERE tribunal_codigo = '13'")
rows = cur.fetchall()
changes = [(cod, nome, normalizar(nome)) for cod, nome in rows if nome != normalizar(nome)]
print(f"   {len(changes)} comarcas com alteração necessária", flush=True)
for cod, old, new in changes:
    print(f"   {cod}: '{old}' -> '{new}'", flush=True)

if changes:
    # UPDATE em massa via VALUES (1 comando só)
    values = ', '.join(f"('{cod}', '{new.replace(chr(39), chr(39)+chr(39))}')" for cod, _, new in changes)
    sql = f"UPDATE cnj_comarcas SET nome = v.novo FROM (VALUES {values}) AS v(cod, novo) WHERE cnj_comarcas.codigo = v.cod AND tribunal_codigo = '13'"
    cur.execute(sql)
    print(f"   → {cur.rowcount} comarcas atualizadas", flush=True)

# ──────────────────────────────────────────────
# 2. Normalizar cnj_comarcas_cidades (1517+ nomes)
# ──────────────────────────────────────────────
print("\n2. Normalizando cnj_comarcas_cidades (cidades + distritos)...", flush=True)
cur.execute("SELECT comarca_oooo, cidade FROM cnj_comarcas_cidades WHERE tribunal_codigo = '13'")
rows = cur.fetchall()
changes = [(comarca, cidade, normalizar(cidade)) for comarca, cidade in rows if cidade != normalizar(cidade)]
print(f"   {len(changes)} cidades/distritos com alteração necessária", flush=True)
for comarca, old, new in changes[:20]:
    print(f"   {comarca}: '{old}' -> '{new}'", flush=True)
if len(changes) > 20:
    print(f"   ... e mais {len(changes)-20}", flush=True)

if changes:
    # UPDATE em massa via VALUES
    values_list = []
    for comarca, old, new in changes:
        # Escapar aspas simples
        old_esc = old.replace("'", "''")
        new_esc = new.replace("'", "''")
        values_list.append(f"('{comarca}', '{old_esc}', '{new_esc}')")
    values = ', '.join(values_list)
    sql = f"""UPDATE cnj_comarcas_cidades SET cidade = v.novo
FROM (VALUES {values}) AS v(comarca_oooo, old, novo)
WHERE cnj_comarcas_cidades.comarca_oooo = v.comarca_oooo
  AND cnj_comarcas_cidades.cidade = v.old
  AND tribunal_codigo = '13'"""
    cur.execute(sql)
    print(f"   → {cur.rowcount} cidades/distritos atualizados", flush=True)

conn.commit()

# ──────────────────────────────────────────────
# 3. Verificar resquícios
# ──────────────────────────────────────────────
print("\n3. Verificando se ainda há acentos/hífens...", flush=True)
cur.execute("SELECT codigo, nome FROM cnj_comarcas WHERE tribunal_codigo = '13'")
ainda = [(r[0], r[1]) for r in cur.fetchall()
         if any(c in r[1] for c in 'ÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇÑ-')]
if ainda:
    for cod, nome in ainda:
        print(f"   ⚠ {cod}: {nome}")
else:
    print("   ✅ Comarcas: 100% normalizadas")

cur.execute("SELECT comarca_oooo, cidade FROM cnj_comarcas_cidades WHERE tribunal_codigo = '13'")
ainda = [(r[0], r[1]) for r in cur.fetchall()
         if any(c in r[1] for c in 'ÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇÑ-')]
if ainda:
    for comarca, cidade in ainda[:10]:
        print(f"   ⚠ {comarca}: {cidade}")
    if len(ainda) > 10:
        print(f"   ... e mais {len(ainda)-10}")
else:
    print("   ✅ Cidades/distritos: 100% normalizados")

print("\nConcluído!", flush=True)
cur.close()
conn.close()