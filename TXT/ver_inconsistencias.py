"""Verificar códigos de comarca inconsistentes no banco local."""
import psycopg2
conn = psycopg2.connect(host='localhost', port=5433, user='postgres', password='postgres', dbname='oficiojus_drive')
cur = conn.cursor()

# Códigos que não existem na tabela cnj_comarcas
cur.execute("""
    SELECT DISTINCT c.comarca_oooo
    FROM cnj_comarcas_cidades c
    LEFT JOIN cnj_comarcas k ON k.codigo = c.comarca_oooo
    WHERE k.codigo IS NULL
    ORDER BY c.comarca_oooo
""")
print('Comarcas em cnj_comarcas_cidades SEM registro em cnj_comarcas:')
for r in cur.fetchall():
    print(f'  {r[0]}')

# Ver se BH tem cidade BH também
cur.execute("SELECT DISTINCT cidade FROM cnj_comarcas_cidades WHERE comarca_oooo = 'BH'")
print('\nCidades da comarca "BH":')
for r in cur.fetchall():
    print(f'  {r[0]}')

# Ver quantas cidades em comarcas com nome BH
cur.execute("SELECT comarca_oooo, COUNT(*) FROM cnj_comarcas_cidades GROUP BY comarca_oooo ORDER BY comarca_oooo")
print('\nTodas as comarcas com nº de cidades:')
for r in cur.fetchall():
    print(f'  {r[0]}: {r[1]} cidades')

conn.close()