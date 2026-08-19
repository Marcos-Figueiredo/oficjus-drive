"""Verificar estrutura dos arquivos no bucket MG/TJMG/."""
from urllib.request import Request, urlopen
import json

SUPABASE_URL = "https://weaqkaaqalvpbxkxrfee.supabase.co"
SUPABASE_ANON_KEY = "sb_publishable_K1A9Oo6uXRAmIXDATMThEw_jDy1fLAJ"
headers = {
    "apikey": SUPABASE_ANON_KEY,
    "Authorization": f"Bearer {SUPABASE_ANON_KEY}",
    "Content-Type": "application/json"
}

# Listar estrutura
for prefixo in ["MG/TJMG/", "MG/TRF6/"]:
    body = json.dumps({"prefix": prefixo, "limit": 500}).encode()
    req = Request(f"{SUPABASE_URL}/storage/v1/object/list/cnefe-data", data=body, headers=headers, method="POST")
    resp = urlopen(req)
    arquivos = json.loads(resp.read())
    print(f"\n=== {prefixo} ({len(arquivos)} itens) ===")
    
    pastas = set()
    arquivos_gz = []
    for a in arquivos:
        name = a.get('name', '?')
        if name.endswith('.ndjson.gz'):
            arquivos_gz.append(name)
        elif '/' in name:
            pasta = name.split('/')[0]
            pastas.add(pasta)
    
    print(f"  Pastas: {len(pastas)}")
    print(f"  Arquivos .ndjson.gz: {len(arquivos_gz)}")
    if arquivos_gz:
        print(f"  Exemplos: {arquivos_gz[:5]}")
    if len(arquivos_gz) < 10:
        # Mostrar todos os itens
        for a in arquivos:
            name = a.get('name', '?')
            sz = a.get('metadata', {}).get('size', 0)
            print(f"    {name} ({sz/1024/1024:.1f} MB)" if sz else f"    {name}")

# Testar uma URL especifica
print("\n=== Testando URLs ===")
for path in ["MG/TJMG/0672/cnefe_unificada.ndjson.gz", "MG/TJMG/0672"]:
    url = f"{SUPABASE_URL}/storage/v1/object/public/cnefe-data/{path}"
    try:
        req = Request(url)
        resp = urlopen(req)
        print(f"  [OK] {url} -> {resp.headers.get('Content-Length', '?')} bytes")
    except Exception as e:
        print(f"  [ERR] {url} -> {e}")