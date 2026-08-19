"""Verificar estrutura dos buckets no Supabase Storage."""
from urllib.request import Request, urlopen
from urllib.error import HTTPError
import json

SUPABASE_URL = "https://weaqkaaqalvpbxkxrfee.supabase.co"
SUPABASE_ANON_KEY = "sb_publishable_K1A9Oo6uXRAmIXDATMThEw_jDy1fLAJ"

headers = {
    "apikey": SUPABASE_ANON_KEY,
    "Authorization": f"Bearer {SUPABASE_ANON_KEY}",
    "Content-Type": "application/json"
}

def listar(bucket, prefixo):
    body = json.dumps({"prefix": prefixo, "limit": 200}).encode()
    req = Request(
        f"{SUPABASE_URL}/storage/v1/object/list/{bucket}",
        data=body, headers=headers, method="POST"
    )
    try:
        resp = urlopen(req)
        data = json.loads(resp.read())
        return data
    except HTTPError as e:
        return f"ERRO {e.code}: {e.read().decode()[:150]}"

print("=== BUCKET cnefe-data (novo) ===")
print("\nRaiz:")
for item in listar("cnefe-data", ""):
    if isinstance(item, dict):
        print(f"  {item.get('name')} {'(pasta)' if item.get('id') is None else ''}")

print("\nMG/:")
for item in listar("cnefe-data", "MG/"):
    if isinstance(item, dict):
        sz = item.get('metadata', {}).get('size', 0)
        print(f"  {item.get('name')} ({sz/1024/1024:.1f} MB)" if sz else f"  {item.get('name')}")

print("\n=== BUCKET oficjus-sync (antigo) ===")
print("\nRaiz:")
for item in listar("oficjus-sync", ""):
    if isinstance(item, dict):
        print(f"  {item.get('name')} {'(pasta)' if item.get('id') is None else ''}")

print("\nMG/:")
for item in listar("oficjus-sync", "MG/"):
    if isinstance(item, dict):
        sz = item.get('metadata', {}).get('size', 0)
        print(f"  {item.get('name')} ({sz/1024/1024:.1f} MB)" if sz else f"  {item.get('name')}")