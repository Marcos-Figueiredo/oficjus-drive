"""Analisar banco SQLite do dispositivo (oficjus_drive_pull.db)."""
import sqlite3

path = r'c:\oficjus-drive\TXT\oficjus_drive_pull.db'
conn = sqlite3.connect(path)
cur = conn.cursor()

# Listar tabelas
cur.execute("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name")
tables = [r[0] for r in cur.fetchall()]
print('Tabelas:', ', '.join(tables))

# Ver enderecos (rota ativa)
if 'enderecos' in tables:
    cur.execute('SELECT COUNT(*) FROM enderecos')
    print(f'\nenderecos: {cur.fetchone()[0]} registros')
    cur.execute(
        "SELECT id, cep, logradouro, numero, bairro, latitude, longitude, "
        "ordem, referencia, rota_grupo_id FROM enderecos ORDER BY referencia"
    )
    for r in cur.fetchall():
        lat = f'{r[5]:.6f}' if r[5] else 'NULL'
        lng = f'{r[6]:.6f}' if r[6] else 'NULL'
        print(f'  id={r[0]} cep={r[1]} log={r[2][:35]!r} num={r[3]} bairro={r[4][:18]!r} '
              f'lat={lat} lng={lng} ordem={r[7]} ref={r[8]} rota={r[9]}')

# Ver rotas
if 'rotas' in tables:
    cur.execute('SELECT * FROM rotas')
    print(f'\nrotas:')
    for r in cur.fetchall():
        print(f'  id={r[0]} nome={r[1]} status={r[3]}')

# Ver cache
if 'logradouro_cache' in tables:
    cur.execute('SELECT COUNT(*) FROM logradouro_cache')
    print(f'\nlogradouro_cache: {cur.fetchone()[0]} registros')
if 'numero_cache' in tables:
    cur.execute('SELECT COUNT(*) FROM numero_cache')
    print(f'numero_cache: {cur.fetchone()[0]} registros')

conn.close()