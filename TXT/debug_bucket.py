"""Verificar estrutura do bucket - modo debug."""
from urllib.request import Request, urlopen
import json

SUPABASE_URL = "https://weaqkaaqalvpbxkxrfee.supabase.co"
SUPABASE_ANON_KEY = "sb_publishable_K1A9Oo6uXRAmIXDATMThEw_jDy1fLAJ"
headers = {
    "apikey": SUPABASE_ANON_KEY,
    "Authorization": f"Bearer {SUPABASE_ANON_KEY}",
    "Content-Type": "application/json"
}

# Listar MG/TJMG/
body = json.dumps({"prefix": "MG/TJMG/", "limit": 1000}).encode()
req = Request(f"{SUPABASE_URL}/storage/v1/object/list/cnefe-data", data=body, headers=headers, method="POST")
resp = urlopen(req)
arquivos = json.loads(resp.read())

print(f"Total itens: {len(arquivos)}")
print()

# Agrupar por tipo
pastas = set()
arquivos_gz = []
vazios = []
for a in arquivos:
    if a is None:
        vazios.append(a)
        continue
    name = a.get('name', '?')
    if name.endswith('.ndjson.gz'):
        arquivos_gz.append(name)
    else:
        # Pode ser pasta ou objeto invalido
        # Se for pasta, o name termina com /
        pastas.add(name)

print(f"Pastas (ou marcadores): {len(pastas)}")
print(f"Arquivos .ndjson.gz: {len(arquivos_gz)}")
print(f"Vazios/nulos: {len(vazios)}")

# Mostrar alguns exemplos
if pastas:
    print(f"\nPrimeiros 10 marcadores:")
    for p in list(pastas)[:10]:
        print(f"  {p}")
if arquivos_gz:
    print(f"\nPrimeiros 5 arquivos .gz:")
    for g in arquivos_gz[:5]:
        print(f"  {g}")

# Verificar TRF6
body2 = json.dumps({"prefix": "MG/TRF6/", "limit": 500}).encode()
req2 = Request(f"{SUPABASE_URL}/storage/v1/object/list/cnefe-data", data=body2, headers=headers, method="POST")
resp2 = urlopen(req2)
trf6 = json.loads(resp2.read())
gz_trf6 = [a.get('name') for a in trf6 if a and a.get('name', '').endswith('.ndjson.gz')]
print(f"\nTRF6: {len(trf6)} itens, {len(gz_trf6)} .ndjson.gz")
if gz_trf6:
    print(f"  Exemplos: {gz_trf6[:5]}")

# Testar URL TJMG
print(f"\nTestando URL TJMG 0672:")
url = f"{SUPABASE_URL}/storage/v1/object/public/cnefe-data/MG/TJMG/0672/cnefe_unificada.ndjson.gz"
try:
    req = Request(url, method="HEAD")
    resp = urlopen(req)
    print(f"  [OK] {resp.status} - {resp.headers.get('Content-Length', '?')} bytes")
except Exception as e:
    print(f"  [ERR] {e}")