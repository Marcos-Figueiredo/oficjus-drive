"""Verificar ITAMOJI no CNEFE."""
import psycopg2
conn = psycopg2.connect(host='localhost', port=5433, user='postgres', password='postgres', dbname='oficiojus_drive')
cur = conn.cursor()
cur.execute("SELECT COUNT(*) FROM cnefe_unificada WHERE cidade = 'ITAMOGI'")
print(f"ITAMOGI no CNEFE: {cur.fetchone()[0]} registros")
cur.execute("SELECT COUNT(*) FROM cnefe_unificada WHERE cidade = 'ITAMOJI'")
print(f"ITAMOJI no CNEFE: {cur.fetchone()[0]} registros")
cur.execute("SELECT cidade, COUNT(*) FROM cnefe_unificada WHERE cidade ILIKE '%ITAMO%' GROUP BY cidade")
for r in cur.fetchall():
    print(f"  {r[0]}: {r[1]} registros")
cur.close()
conn.close()