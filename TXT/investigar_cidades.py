"""Investigar nomes de cidades no original_ibge vs mapeamento de comarcas."""
import psycopg2

conn = psycopg2.connect(host='localhost', port=5433, user='postgres', password='postgres', dbname='oficiojus_drive')
cur = conn.cursor()

# 1. Como são os nomes de cidades no original_ibge (sem acento?)
cur.execute("SELECT DISTINCT dsc_localidade FROM original_ibge ORDER BY dsc_localidade LIMIT 30")
print("Nomes de cidades no original_ibge:")
for r in cur.fetchall():
    print(f"  {r[0]}")

# 2. Ver os cod_municipio de ITAMOJI e PASSA-QUATRO
# IBGE: ITAMOJI código ~3137108, PASSA-QUATRO ~3147606
cur.execute("SELECT DISTINCT cod_municipio, dsc_localidade FROM original_ibge WHERE cod_municipio IN ('3137108', '3147606') LIMIT 10")
print("\nPor cod_municipio (3137108=ITAMOJI, 3147606=PASSA-QUATRO):")
for r in cur.fetchall():
    print(f"  {r[0]} {r[1]}")

# 3. Procurar PINHEIRINHOS no original_ibge
cur.execute("SELECT cod_municipio, dsc_localidade, COUNT(*) FROM original_ibge WHERE dsc_localidade LIKE '%PINHEIRINHO%' GROUP BY 1,2")
print("\nPINHEIRINHOS no original_ibge:")
for r in cur.fetchall():
    print(f"  cod={r[0]} cidade={r[1]} n={r[2]}")

# 4. Ver todos os municípios que não têm correspondência no cnefe_unificada
# Comparar cidades das comarcas vs cidades do CNEFE
cur.execute("""
    SELECT DISTINCT dsc_localidade FROM original_ibge 
    WHERE cod_uf = '31'
    ORDER BY dsc_localidade
""")
cidades_ibge = {r[0] for r in cur.fetchall()}

cur.execute("SELECT DISTINCT cidade FROM cnefe_unificada")
cidades_cnefe = {r[0] for r in cur.fetchall()}

print(f"\nCidades no original_ibge (MG): {len(cidades_ibge)}")
print(f"Cidades no cnefe_unificada: {len(cidades_cnefe)}")
print(f"Diferença (IBGE mas não no CNEFE): {len(cidades_ibge - cidades_cnefe)}")
# Amostra
dif = sorted(cidades_ibge - cidades_cnefe)
print(f"Exemplos: {dif[:20]}")

cur.close()
conn.close()