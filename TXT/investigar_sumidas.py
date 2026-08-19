"""Investigar as 3 comarcas sumidas e se há mais comarcas no TJMG que não estão no Supabase."""
import psycopg2, json

# 1. Verificar comarcas no JSON original (fonte TJMG)
with open(r'c:\oficjus-drive\TXT\comarcas-tjmg-oficial.json', encoding='utf-8') as f:
    comarcas_json = json.load(f)

print(f"Comarcas no JSON TJMG: {len(comarcas_json)}")
print(f"Ativas no JSON: {sum(1 for c in comarcas_json if c.get('ativo'))}")

# 2. Verificar no Supabase
conn = psycopg2.connect(host='aws-0-us-west-2.pooler.supabase.com', port=5432, dbname='postgres',
    user='postgres.weaqkaaqalvpbxkxrfee', password='Senha14498620@', connect_timeout=30)
cur = conn.cursor()
cur.execute("SELECT codigo, nome FROM cnj_comarcas WHERE tribunal_codigo = '13' ORDER BY codigo")
supabase = {r[0]: r[1] for r in cur.fetchall()}
print(f"Comarcas no Supabase: {len(supabase)}")

# 3. Comparar: JSON vs Supabase
codigos_json = {c['codigo'] for c in comarcas_json if c.get('ativo')}
codigos_supabase = set(supabase.keys())

# Comarcas no JSON que NÃO estão no Supabase
faltam_supabase = sorted(codigos_json - codigos_supabase)
print(f"\nComarcas no JSON TJMG que FALTAM no Supabase ({len(faltam_supabase)}):")
for cod in faltam_supabase:
    j = next(c for c in comarcas_json if c['codigo'] == cod)
    print(f"  {cod} {j['nome']} (ordem={j.get('ordem')})")

# Comarcas no Supabase que NÃO estão no JSON
sobras_supabase = sorted(codigos_supabase - codigos_json)
print(f"\nComarcas no Supabase extras (sem no JSON) ({len(sobras_supabase)}):")
for cod in sobras_supabase:
    print(f"  {cod} {supabase[cod]}")

# 4. Verificar se ITAMOJI e PASSA-QUATRO existem no JSON
print("\nVerificando comarcas específicas:")
for cod in ['0329', '0476', '0280']:
    in_json = any(c['codigo'] == cod for c in comarcas_json)
    in_supabase = cod in supabase
    print(f"  {cod}: JSON={in_json}, Supabase={in_supabase}")

# 5. Verificar se essas cidades existem no original_ibge
conn2 = psycopg2.connect(host='localhost', port=5433, user='postgres', password='postgres', dbname='oficiojus_drive')
cur2 = conn2.cursor()
for cod in ['0329', '0476']:
    cur2.execute("SELECT cidade FROM cnj_comarcas_cidades WHERE comarca_oooo = %s", (cod,))
    cidades = [r[0].upper() for r in cur2.fetchall()]
    if cidades:
        for cidade in cidades:
            cur2.execute("SELECT COUNT(*) FROM original_ibge WHERE dsc_localidade = %s", (cidade,))
            total = cur2.fetchone()[0]
            print(f"  {cod} {cidade}: {total} registros no original_ibge")
    else:
        print(f"  {cod}: SEM CIDADES no mapeamento")
cur2.close()
conn2.close()

cur.close()
conn.close()