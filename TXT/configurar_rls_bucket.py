"""Criar políticas RLS para o bucket oficjus-sync"""
import psycopg2
conn = psycopg2.connect(
    host='aws-0-us-west-2.pooler.supabase.com',
    port=5432,
    dbname='postgres',
    user='postgres.weaqkaaqalvpbxkxrfee',
    password='Senha14498620@',
    connect_timeout=30
)
cur = conn.cursor()

bucket_id = 'oficjus-sync'

# Remover políticas antigas específicas deste bucket
for policy in ['oficjus_sync_select_anon', 'oficjus_sync_insert_auth', 'oficjus_sync_update_auth', 'oficjus_sync_delete_auth']:
    cur.execute(f"DROP POLICY IF EXISTS \"{policy}\" ON storage.objects;")
print('Politicas antigas removidas')

# 1. SELECT: qualquer um pode ler (anon + authenticated) - igual cnefe_logradouros
cur.execute(f"""
    CREATE POLICY "oficjus_sync_select_anon"
    ON storage.objects
    FOR SELECT
    TO anon, authenticated
    USING (bucket_id = '{bucket_id}');
""")
print('SELECT: anon + authenticated')

# 2. INSERT: apenas authenticated
cur.execute(f"""
    CREATE POLICY "oficjus_sync_insert_auth"
    ON storage.objects
    FOR INSERT
    TO authenticated
    WITH CHECK (bucket_id = '{bucket_id}');
""")
print('INSERT: apenas authenticated')

# 3. UPDATE: apenas authenticated
cur.execute(f"""
    CREATE POLICY "oficjus_sync_update_auth"
    ON storage.objects
    FOR UPDATE
    TO authenticated
    USING (bucket_id = '{bucket_id}')
    WITH CHECK (bucket_id = '{bucket_id}');
""")
print('UPDATE: apenas authenticated')

# 4. DELETE: apenas authenticated
cur.execute(f"""
    CREATE POLICY "oficjus_sync_delete_auth"
    ON storage.objects
    FOR DELETE
    TO authenticated
    USING (bucket_id = '{bucket_id}');
""")
print('DELETE: apenas authenticated')

conn.commit()

# Verificar
cur.execute("""
    SELECT policyname, cmd, roles
    FROM pg_policies
    WHERE schemaname = 'storage' AND tablename = 'objects'
    ORDER BY policyname
""")
print('\n=== Politicas do storage ===')
for r in cur.fetchall():
    print(f'  {r[0]}: cmd={r[1]}, roles={r[2]}')

cur.close()
conn.close()
print('\n✅ Segurança configurada!')