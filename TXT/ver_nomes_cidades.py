"""Verificar nomes reais de cidades no CNEFE para PASSA-QUATRO e ITAMOJI."""
import psycopg2

conn = psycopg2.connect(host='localhost', port=5433, user='postgres', password='postgres', dbname='oficiojus_drive')
cur = conn.cursor()

# Ver todos os municípios de MG no cnefe_unificada (853 cidades)
cur.execute("SELECT DISTINCT cidade FROM cnefe_unificada ORDER BY cidade")
cidades = [r[0] for r in cur.fetchall()]
print(f"Total cidades no CNEFE: {len(cidades)}")

# Procurar parecidas com PASSA-QUATRO, ITAMOJI, PINHEIRINHO
for termo in ['PASSA', 'ITAMO', 'PINHEIRINH', 'PÉ DO', 'PE DO']:
    print(f"\nCidades contendo '{termo}':")
    for c in cidades:
        if termo.upper() in c.upper():
            print(f"  {c}")

# Ver os códigos IBGE destas cidades
print("\nCom posição no código IBGE:")
cur.execute("""
    SELECT DISTINCT cod_municipio, dsc_localidade 
    FROM original_ibge 
    WHERE dsc_localidade IN ('PASSA-QUATRO', 'PASSA QUATRO', 'ITAMOJI', 'PINHEIRINHOS', 'PINHEIRINHO', 'PÉ DO MORRO')
""")
for r in cur.fetchall():
    print(f"  {r[0]} {r[1]}")

# Ver cod_municipio de PASSA-QUATRO (3147606) e ITAMOJI (3137108)
print("\nDados do CNEFE para os códigos IBGE:")
cur.execute("""
    SELECT DISTINCT cod_municipio, dsc_localidade 
    FROM original_ibge 
    WHERE cod_municipio IN ('3137108', '3147606', '3147600')
""")
for r in cur.fetchall():
    print(f"  {r[0]} {r[1]}")

cur.close()
conn.close()