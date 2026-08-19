import subprocess, os, sqlite3

adb = os.path.expandvars(r'%USERPROFILE%\AppData\Local\Android\Sdk\platform-tools\adb.exe')

# 1) Force WAL checkpoint via Room close — just copy the DB directly
r = subprocess.run([
    adb, 'exec-out', 'run-as', 'br.com.oficjus.drive',
    'cat', '/data/data/br.com.oficjus.drive/databases/oficjus_drive.db'
], capture_output=True)

db_path = r'C:\Users\marco\AppData\Local\Temp\oficjus_fresh.db'
with open(db_path, 'wb') as f:
    f.write(r.stdout)

print(f'Copiado: {len(r.stdout)} bytes')
print(f'Header: {r.stdout[:16]}')

conn = sqlite3.connect(db_path)
c = conn.execute('SELECT COUNT(*) FROM logradouro_cache')
print(f'Logradouros (mae): {c.fetchone()[0]}')
c = conn.execute('SELECT COUNT(*) FROM numero_cache')
print(f'Numeros (filha): {c.fetchone()[0]}')

c = conn.execute('SELECT substr(cep,1,4), COUNT(*) FROM numero_cache GROUP BY 1 ORDER BY 1')
print('Numeros por prefixo CEP:')
for row in c.fetchall():
    print(f'  {row[0]}: {row[1]}')
conn.close()