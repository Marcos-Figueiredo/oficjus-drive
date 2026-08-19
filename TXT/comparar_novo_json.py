"""Comparar novo JSON (339 comarcas) com nossa base (295)."""
import json, psycopg2

# Novo JSON
d = json.load(open(r'c:\Users\marco\Downloads\comarcas_tjmg (1).json', encoding='utf-8'))
novo = {c['codigo']: c for c in d['comarcas']}
print(f"Novo JSON: {len(novo)} comarcas")

# Ver estrutura de uma comarca
print(f"\nEstrutura de exemplo:")
print(json.dumps(d['comarcas'][0], ensure_ascii=False, indent=2)[:500])

# Nossa base
conn = psycopg2.connect(host='aws-0-us-west-2.pooler.supabase.com', port=5432, dbname='postgres',
    user='postgres.weaqkaaqalvpbxkxrfee', password='Senha14498620@', connect_timeout=30)
cur = conn.cursor()
cur.execute("SELECT codigo, nome FROM cnj_comarcas WHERE tribunal_codigo = '13'")
nossa = {r[0]: r[1] for r in cur.fetchall()}
cur.close()
conn.close()
print(f"Nossa base: {len(nossa)}")

# Faltam na nossa base
codigos_novo = set(novo.keys())
codigos_nossa = set(nossa.keys())
faltam = sorted(codigos_novo - codigos_nossa)
print(f"\nComarcas no novo JSON que FALTAM na nossa base ({len(faltam)}):")
for c in faltam:
    info = novo[c]
    muns = info.get('municipios_integrantes') or info.get('municipios') or []
    print(f"  {c} {info.get('nome')} - muns: {muns}")

# Extras
sobras = sorted(codigos_nossa - codigos_novo)
print(f"\nExtras na nossa base ({len(sobras)}):")
for c in sobras:
    print(f"  {c} {nossa[c]}")