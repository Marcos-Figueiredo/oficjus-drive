"""Verificar comarcas no CSV original (original_ibge) e comparar com as tabelas."""
import psycopg2

conn = psycopg2.connect(host='localhost', port=5433, user='postgres', password='postgres', dbname='oficiojus_drive')
cur = conn.cursor()

# Estrutura original_ibge
cur.execute("SELECT column_name FROM information_schema.columns WHERE table_name = 'original_ibge' ORDER BY ordinal_position")
cols = [r[0] for r in cur.fetchall()]
print(f"Colunas original_ibge: {cols}")

# Contar registros
cur.execute("SELECT COUNT(*) FROM original_ibge")
print(f"Registros: {cur.fetchone()[0]:,}")

# Ver cidades distintas
cur.execute("SELECT COUNT(DISTINCT cidade) FROM original_ibge")
print(f"Cidades distintas: {cur.fetchone()[0]}")

# Ver algumas amostras
cur.execute("SELECT DISTINCT cidade FROM original_ibge ORDER BY cidade LIMIT 30")
print("\nPrimeiras cidades:")
for r in cur.fetchall():
    print(f"  {r[0]}")

cur.close()
conn.close()