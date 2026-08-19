"""Verificar comarca de Betim no banco local."""
import psycopg2
conn = psycopg2.connect(host='localhost', port=5433, user='postgres', password='postgres', dbname='oficiojus_drive')
cur = conn.cursor()

cur.execute("SELECT comarca_oooo, cidade FROM cnj_comarcas_cidades WHERE cidade ILIKE '%BETIM%'")
print('cnj_comarcas_cidades com BETIM:')
for r in cur.fetchall():
    print(f'  comarca={r[0]} cidade={r[1]}')

cur.execute("SELECT codigo, nome FROM cnj_comarcas WHERE nome ILIKE '%BETIM%'")
print('cnj_comarcas com BETIM:')
for r in cur.fetchall():
    print(f'  codigo={r[0]} nome={r[1]}')

# Ver comarca 0024 (BH) completa
cur.execute("SELECT comarca_oooo, cidade, tribunal_codigo FROM cnj_comarcas_cidades WHERE comarca_oooo = '0024'")
print('\nComarca 0024 (BH):')
for r in cur.fetchall():
    print(f'  cidade={r[1]}')

conn.close()