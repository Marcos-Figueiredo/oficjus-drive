"""Normalizar cidades do mapeamento para os nomes do CNEFE e reprocessar comarcas afetadas."""
import psycopg2, unicodedata, re, json, time, gzip, io

def normalizar(texto):
    """Remove acentos e troca hífens/outros por espaço (padrão CNEFE/IBGE)."""
    t = unicodedata.normalize('NFD', texto)
    t = ''.join(c for c in t if unicodedata.category(c) != 'Mn')
    t = t.upper()
    t = re.sub(r'[^A-Z0-9 ]', ' ', t)
    t = re.sub(r'\s+', ' ', t).strip()
    return t

conn = psycopg2.connect(host='localhost', port=5433, user='postgres', password='postgres', dbname='oficiojus_drive')
cur = conn.cursor()

# 1. Índice de cidades CNEFE por nome normalizado
cur.execute("SELECT DISTINCT cidade FROM cnefe_unificada")
cidades_cnefe = {r[0] for r in cur.fetchall()}
norm_cnefe = {}
for c in cidades_cnefe:
    norm_cnefe.setdefault(normalizar(c), c)
print(f"Cidades CNEFE: {len(cidades_cnefe)}")

# 2. Mapeamento de comarcas → cidades (com normalização)
cur.execute("""
    SELECT DISTINCT comarca_oooo, cidade
    FROM cnj_comarcas_cidades
    WHERE tribunal_codigo = '13'
""")
mapeamento = cur.fetchall()
print(f"Mapeamento comarcas: {len(mapeamento)} registros")

# 3. Construir mapa comarca → lista de cidades CNEFE (normalizadas)
from collections import defaultdict
comarca_cidades_cnefe = defaultdict(set)
comarca_cidades_originais = defaultdict(list)
desnormalizadas = []  # cidades que só bateram após normalização

for comarca, cidade in mapeamento:
    comarca_cidades_originais[comarca].append(cidade)
    cidade_upper = cidade.upper()
    if cidade_upper in cidades_cnefe:
        comarca_cidades_cnefe[comarca].add(cidade_upper)
    else:
        n = normalizar(cidade)
        if n in norm_cnefe:
            comarca_cidades_cnefe[comarca].add(norm_cnefe[n])
            desnormalizadas.append((comarca, cidade, norm_cnefe[n]))
        # se não encontrou: é distrito/localidade, dados estão sob o município

print(f"Cidades que bateram só com normalização: {len(desnormalizadas)}")
for c, orig, cnefe in sorted(desnormalizadas):
    print(f"  {c} '{orig}' -> '{cnefe}'")

# 4. Ver comarcas que ficaram SEM nenhuma cidade CNEFE
print("\nComarcas sem cidade CNEFE (após normalização):")
sem_dados = []
for comarca in sorted(comarca_cidades_cnefe.keys()):
    if not comarca_cidades_cnefe[comarca]:
        sem_dados.append(comarca)
for comarca in sorted(comarca_cidades_originais.keys()):
    if comarca not in comarca_cidades_cnefe:
        sem_dados.append(comarca)

for comarca in sorted(set(sem_dados)):
    cur.execute("SELECT nome FROM cnj_comarcas WHERE codigo = %s", (comarca,))
    nome = cur.fetchone()
    nome = nome[0] if nome else '?'
    print(f"  {comarca} {nome}: cidades mapeadas = {comarca_cidades_originais.get(comarca, [])}")

cur.close()
conn.close()