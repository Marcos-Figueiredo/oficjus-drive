"""Contar comarcas TJMG no Supabase (tabela, não bucket)."""
import psycopg2

conn = psycopg2.connect(host='aws-0-us-west-2.pooler.supabase.com', port=5432, dbname='postgres',
    user='postgres.weaqkaaqalvpbxkxrfee', password='Senha14498620@', connect_timeout=30)
cur = conn.cursor()

cur.execute("SELECT COUNT(*) FROM cnj_comarcas WHERE tribunal_codigo = '13'")
print("Total TJMG na tabela:", cur.fetchone()[0])

cur.execute("SELECT COUNT(*) FROM cnj_comarcas WHERE tribunal_codigo = '13' AND ativo = true")
print("Ativas:", cur.fetchone()[0])

cur.execute("SELECT COUNT(*) FROM cnj_comarcas WHERE tribunal_codigo = '13' AND ativo = false")
print("Inativas:", cur.fetchone()[0])

# Listar as comarcas para conferir
cur.execute("SELECT codigo, nome FROM cnj_comarcas WHERE tribunal_codigo = '13' ORDER BY codigo")
comarcas = cur.fetchall()
print(f"\nLista ({len(comarcas)}):")
for r in comarcas:
    print(f"  {r[0]} {r[1]}")

cur.close()
conn.close()