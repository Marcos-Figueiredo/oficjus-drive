"""Verificar tabelas TRF6 no banco local."""
import psycopg2, json
conn = psycopg2.connect(host='localhost',port=5433,user='postgres',password='postgres',dbname='oficiojus_drive')
cur = conn.cursor()
cur.execute("SELECT table_name FROM information_schema.tables WHERE table_schema='public' ORDER BY table_name")
for r in cur.fetchall():
    print(r[0])
cur.close()
conn.close()