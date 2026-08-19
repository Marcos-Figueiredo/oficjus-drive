"""Verificar quantas cidades do mapeamento não batem com o CNEFE por normalização."""
import psycopg2, unicodedata, re

def normalizar(texto):
    """Remove acentos, hífens, e normaliza para comparação."""
    t = unicodedata.normalize('NFD', texto)
    t = ''.join(c for c in t if unicodedata.category(c) != 'Mn')
    t = t.upper()
    t = re.sub(r'[^A-Z0-9 ]', ' ', t)  # troca hífen/outros por espaço
    t = re.sub(r'\s+', ' ', t).strip()
    return t

conn = psycopg2.connect(host='localhost', port=5433, user='postgres', password='postgres', dbname='oficiojus_drive')
cur = conn.cursor()

# Cidades do CNEFE normalizadas
cur.execute("SELECT DISTINCT cidade FROM cnefe_unificada")
cidades_cnefe = {r[0] for r in cur.fetchall()}
norm_cnefe = {normalizar(c): c for c in cidades_cnefe}

# Cidades do mapeamento de comarcas
cur.execute("""
    SELECT DISTINCT c.comarca_oooo, c.cidade
    FROM cnj_comarcas_cidades c
    WHERE c.tribunal_codigo = '13'
""")
mapeamento = cur.fetchall()

print(f"Cidades CNEFE: {len(cidades_cnefe)}")
print(f"Mapeamento comarcas: {len(mapeamento)}")

# Ver quais cidades do mapeamento NÃO existem nem normalizadas
nao_encontradas = set()
com_normalizacao = 0
for comarca, cidade in mapeamento:
    if cidade.upper() not in cidades_cnefe:
        norm = normalizar(cidade)
        if norm in norm_cnefe:
            com_normalizacao += 1
        else:
            nao_encontradas.add((comarca, cidade))

print(f"Cidades que batem só com normalização: {com_normalizacao}")
print(f"Cidades que NÃO existem nem normalizando: {len(nao_encontradas)}")

# Listar as que não existem
print("\nCidades sem correspondência no CNEFE:")
for comarca, cidade in sorted(nao_encontradas):
    print(f"  {comarca} {cidade}")

# Mostrar exemplos de normalização
print("\nExemplos de cidades que batem com normalização:")
cont = 0
for comarca, cidade in mapeamento:
    if cidade.upper() not in cidades_cnefe:
        norm = normalizar(cidade)
        if norm in norm_cnefe:
            print(f"  {cidade} -> {norm_cnefe[norm]}")
            cont += 1
            if cont >= 30:
                break

cur.close()
conn.close()