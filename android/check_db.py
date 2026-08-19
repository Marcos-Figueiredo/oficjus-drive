import sqlite3

conn = sqlite3.connect(r"C:\Users\marco\AppData\Local\Temp\oficjus6.db")
c = conn.execute("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name")
tables = [r[0] for r in c.fetchall()]
print('Tabelas:', tables)
for t in tables:
    c2 = conn.execute(f'SELECT COUNT(*) FROM "{t}"')
    print(f'  {t}: {c2.fetchone()[0]} reg')
conn.close()