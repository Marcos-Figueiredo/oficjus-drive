"""Comparar JSON oficial do TJMG (298 comarcas) com nossa base atual."""
import json, psycopg2

# JSON oficial do TJMG
oficial = json.load(open(r'c:\Users\marco\Downloads\comarcas_tjmg.json', encoding='utf-8'))
print(f"JSON oficial: {oficial['total_comarcas']} comarcas, {len(oficial['comarcas'])} no array")

# Mapa: código -> nome
tjmg_oficial = {c['codigo']: c for c in oficial['comarcas']}
print(f"Comarcas no JSON: {len(tjmg_oficial)}")

# Nossa base (Supabase)
conn = psycopg2.connect(host='aws-0-us-west-2.pooler.supabase.com', port=5432, dbname='postgres',
    user='postgres.weaqkaaqalvpbxkxrfee', password='Senha14498620@', connect_timeout=30)
cur = conn.cursor()
cur.execute("SELECT codigo, nome FROM cnj_comarcas WHERE tribunal_codigo = '13'")
nossa_base = {r[0]: r[1] for r in cur.fetchall()}
cur.close()
conn.close()
print(f"Nossa base (Supabase): {len(nossa_base)}")

# 1. Comarcas que estão no oficial mas NÃO na nossa base
codigos_oficial = set(tjmg_oficial.keys())
codigos_nossa = set(nossa_base.keys())
faltam = sorted(codigos_oficial - codigos_nossa)
print(f"\nComarcas que FALTAM na nossa base ({len(faltam)}):")
for c in faltam:
    info = tjmg_oficial[c]
    print(f"  {c} {info['nome']} - municipios: {info['municipios_integrantes']}")

# 2. Comarcas que estão na nossa base mas NÃO no oficial
sobras = sorted(codigos_nossa - codigos_oficial)
print(f"\nComarcas EXTRAS na nossa base ({len(sobras)}):")
for c in sobras:
    print(f"  {c} {nossa_base[c]}")

# 3. Diferenças nos nomes
print(f"\nDiferenças de nomes (mesmo código, nome diferente):")
for c in sorted(codigos_oficial & codigos_nossa):
    nome_oficial = tjmg_oficial[c]['nome'].upper()
    nome_nosso = nossa_base[c].upper()
    if nome_oficial != nome_nosso:
        print(f"  {c}: oficial='{nome_oficial}' nossa='{nome_nosso}'")

# 4. Diferenças nos municipios integrantes
print(f"\nComparando municipios integrantes (amostra):")
from collections import Counter
cidades_certas = 0
cidades_diferentes = 0
for c in sorted(codigos_oficial & codigos_nossa):
    muns_oficial = set(m.upper() for m in tjmg_oficial[c]['municipios_integrantes'])
    muns_nossa = set()
    conn2 = psycopg2.connect(host='localhost', port=5433, user='postgres', password='postgres', dbname='oficiojus_drive')
    cur2 = conn2.cursor()
    cur2.execute("SELECT cidade FROM cnj_comarcas_cidades WHERE comarca_oooo = %s", (c,))
    muns_nossa = set(r[0].upper() for r in cur2.fetchall())
    cur2.close()
    conn2.close()
    
    if muns_oficial == muns_nossa:
        cidades_certas += 1
    else:
        cidades_diferentes += 1
        if cidades_diferentes <= 10:
            print(f"  {c} {tjmg_oficial[c]['nome']}:")
            print(f"    Oficial: {sorted(muns_oficial)}")
            print(f"    Nossa:   {sorted(muns_nossa)}")

print(f"\nComarcas com municipios iguais: {cidades_certas}")
print(f"Comarcas com municipios diferentes: {cidades_diferentes}")