"""Aplicar políticas RLS para o bucket cnefe-data no Supabase (mesmo padrão do oficjus-sync)."""
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

bucket_id = 'cnefe-data'

# Remover políticas antigas específicas deste bucket
for policy in ['cnefe_data_select_anon', 'cnefe_data_insert_auth', 'cnefe_data_update_auth', 'cnefe_data_delete_auth']:
    cur.execute(f'DROP POLICY IF EXISTS "{policy}" ON storage.objects;')
print('Políticas antigas removidas')

# 1. SELECT: qualquer um pode ler (anon + authenticated)
cur.execute(f"""
    CREATE POLICY "cnefe_data_select_anon"
    ON storage.objects
    FOR SELECT
    TO anon, authenticated
    USING (bucket_id = '{bucket_id}');
""")
print('SELECT: anon + authenticated')

# 2. INSERT: apenas authenticated
cur.execute(f"""
    CREATE POLICY "cnefe_data_insert_auth"
    ON storage.objects
    FOR INSERT
    TO authenticated
    WITH CHECK (bucket_id = '{bucket_id}');
""")
print('INSERT: apenas authenticated')

# 3. UPDATE: apenas authenticated
cur.execute(f"""
    CREATE POLICY "cnefe_data_update_auth"
    ON storage.objects
    FOR UPDATE
    TO authenticated
    USING (bucket_id = '{bucket_id}')
    WITH CHECK (bucket_id = '{bucket_id}');
""")
print('UPDATE: apenas authenticated')

# 4. DELETE: apenas authenticated
cur.execute(f"""
    CREATE POLICY "cnefe_data_delete_auth"
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
print('\n=== Políticas do storage ===')
for r in cur.fetchall():
    print(f'  {r[0]}: cmd={r[1]}, roles={r[2]}')

cur.close()
conn.close()
print('\n✅ Segurança configurada!')