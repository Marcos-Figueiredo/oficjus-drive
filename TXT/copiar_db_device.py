"""Copiar banco do dispositivo e analisar (via subprocess para preservar binário)."""
import subprocess
import sqlite3
import os

ADB = os.path.expandvars(r'%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe')
SERIAL = 'P80230516015368'
DEST = r'c:\oficjus-drive\TXT\db_device.sqlite'

# 1. Copiar via exec-out (binário puro, sem corrupção do terminal)
cmd = [ADB, '-s', SERIAL, 'exec-out', 'run-as', 'br.com.oficjus.drive', 'cat', 'databases/oficjus_drive.db']
with open(DEST, 'wb') as f:
    subprocess.run(cmd, stdout=f, check=True)

print(f'Banco copiado: {os.path.getsize(DEST)} bytes')

# 2. Analisar
conn = sqlite3.connect(DEST)
cur = conn.cursor()

cur.execute("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name")
tables = [r[0] for r in cur.fetchall()]
print('Tabelas:', ', '.join(tables))

if 'enderecos' in tables:
    cur.execute('SELECT COUNT(*) FROM enderecos')
    total = cur.fetchone()[0]
    print(f'\nenderecos: {total} registros')
    cur.execute(
        "SELECT id, cep, logradouro, numero, bairro, latitude, longitude, "
        "ordem, referencia FROM enderecos ORDER BY referencia"
    )
    for r in cur.fetchall():
        lat = f'{r[5]:.6f}' if r[5] else 'NULL'
        lng = f'{r[6]:.6f}' if r[6] else 'NULL'
        print(f'  id={r[0]} cep={r[1]} log={r[2][:35]} num={r[3]} bairro={r[4][:18]} '
              f'lat={lat} lng={lng} ordem={r[7]} ref={r[8]}')

if 'rotas' in tables:
    cur.execute('SELECT * FROM rotas')
    for r in cur.fetchall():
        print(f'rota: id={r[0]} nome={r[1]} status={r[3]}')

if 'logradouro_cache' in tables:
    cur.execute('SELECT COUNT(*) FROM logradouro_cache')
    print(f'logradouro_cache: {cur.fetchone()[0]} registros')
if 'numero_cache' in tables:
    cur.execute('SELECT COUNT(*) FROM numero_cache')
    print(f'numero_cache: {cur.fetchone()[0]} registros')

conn.close()