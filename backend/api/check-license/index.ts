// ============================================================
// check-license — Edge Function Supabase
// Verifica se o usuário tem licença válida para usar o app.
// Chamada pelo app Android no login.
// ============================================================

import { serve } from 'https://deno.land/std@0.168.0/http/server.ts';
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2';

serve(async (req) => {
  try {
    const authHeader = req.headers.get('Authorization');
    if (!authHeader) {
      return new Response(JSON.stringify({ error: 'Token não fornecido' }), { status: 401 });
    }

    const supabase = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_ANON_KEY') ?? '',
      { global: { headers: { Authorization: authHeader } } }
    );

    const { data: { user }, error: userError } = await supabase.auth.getUser();
    if (userError || !user) {
      return new Response(JSON.stringify({ error: 'Usuário não autenticado' }), { status: 401 });
    }

    const { data: profile, error: profileError } = await supabase
      .from('profiles')
      .select('*')
      .eq('id', user.id)
      .single();

    if (profileError || !profile) {
      return new Response(JSON.stringify({ error: 'Perfil não encontrado' }), { status: 404 });
    }

    // Regras de licenciamento
    const status = profile.status_assinatura;
    const now = new Date();
    let valido = false;
    let motivo = null;
    let diasRestantes = 0;

    if (status === 'ativo') {
      valido = true;
    } else if (status === 'vitalicio') {
      valido = true;
    } else if (status === 'trial') {
      const trialFim = profile.trial_fim
        ? new Date(profile.trial_fim)
        : new Date(new Date(profile.trial_inicio).getTime() + 7 * 24 * 60 * 60 * 1000);

      if (now <= trialFim) {
        valido = true;
        diasRestantes = Math.ceil((trialFim.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));
      } else {
        // Grace period de 5 dias
        const graceFim = new Date(trialFim.getTime() + 5 * 24 * 60 * 60 * 1000);
        if (now <= graceFim) {
          valido = true;
          motivo = `Trial expirou em ${trialFim.toLocaleDateString('pt-BR')}. Assine para continuar.`;
        } else {
          motivo = 'Período de teste expirou. Assine o OficJus Drive para continuar.';
        }
      }
    } else if (status === 'inadimplente') {
      motivo = 'Assinatura pendente de pagamento. Regularize para continuar.';
    } else if (status === 'cancelado') {
      motivo = 'Assinatura cancelada. Renove para continuar usando.';
    }

    return new Response(JSON.stringify({
      valido,
      motivo,
      status,
      plano: profile.plano || 'drive',
      diasRestantes,
      trialFim: profile.trial_fim,
    }), {
      headers: { 'Content-Type': 'application/json' },
    });
  } catch (err) {
    return new Response(JSON.stringify({ error: err.message }), { status: 500 });
  }
});