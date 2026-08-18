// ============================================================
// register-account — Edge Function Supabase
// Cria conta, seta trial e retorna dados do usuário.
// ============================================================

import { serve } from 'https://deno.land/std@0.168.0/http/server.ts';
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2';

serve(async (req) => {
  try {
    const { email, password, full_name } = await req.json();

    if (!email || !password || !full_name) {
      return new Response(JSON.stringify({ error: 'Campos obrigatórios: email, password, full_name' }), { status: 400 });
    }

    const supabase = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? '',
    );

    // Cria usuário no Auth
    const { data: authData, error: authError } = await supabase.auth.admin.createUser({
      email,
      password,
      email_confirm: true,
      user_metadata: {
        full_name,
        plano: 'drive',
        trial_start: new Date().toISOString(),
        trial_end: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString(),
        status_assinatura: 'trial',
      },
    });

    if (authError) throw authError;

    // Atualiza profile
    const { error: profileError } = await supabase
      .from('profiles')
      .update({
        full_name,
        plano: 'drive',
        trial_inicio: new Date().toISOString(),
        trial_fim: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString(),
        status_assinatura: 'trial',
      })
      .eq('id', authData.user.id);

    if (profileError) throw profileError;

    return new Response(JSON.stringify({
      success: true,
      user: {
        id: authData.user.id,
        email: authData.user.email,
        trial_fim: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString(),
      },
    }), {
      headers: { 'Content-Type': 'application/json' },
    });
  } catch (err) {
    return new Response(JSON.stringify({ error: err.message }), { status: 500 });
  }
});