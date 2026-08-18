// ============================================================
// webhook — Edge Function Supabase
// Recebe notificações do Asaas sobre pagamentos.
// ============================================================

import { serve } from 'https://deno.land/std@0.168.0/http/server.ts';
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2';

serve(async (req) => {
  try {
    const body = await req.json();
    const { event, payment } = body;

    if (!event || !payment) {
      return new Response(JSON.stringify({ error: 'Payload inválido' }), { status: 400 });
    }

    const supabase = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? '',
    );

    // Busca o customer_id do Asaas no profile
    const { data: profile } = await supabase
      .from('profiles')
      .select('id')
      .eq('gateway_customer_id', payment.customer)
      .single();

    if (!profile) {
      return new Response(JSON.stringify({ error: 'Cliente não encontrado' }), { status: 404 });
    }

    switch (event) {
      case 'PAYMENT_RECEIVED':
        // Pagamento confirmado → ativa assinatura
        await supabase
          .from('profiles')
          .update({
            status_assinatura: 'ativo',
            assinatura_inicio: new Date().toISOString(),
            assinatura_fim: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString(),
            ultimo_pagamento: new Date().toISOString(),
          })
          .eq('id', profile.id);
        break;

      case 'PAYMENT_OVERDUE':
        // Pagamento atrasado → marca como inadimplente
        await supabase
          .from('profiles')
          .update({ status_assinatura: 'inadimplente' })
          .eq('id', profile.id);
        break;

      case 'PAYMENT_REFUNDED':
      case 'SUBSCRIPTION_CANCELED':
        // Cancelado
        await supabase
          .from('profiles')
          .update({ status_assinatura: 'cancelado' })
          .eq('id', profile.id);
        break;
    }

    return new Response(JSON.stringify({ success: true }), {
      headers: { 'Content-Type': 'application/json' },
    });
  } catch (err) {
    return new Response(JSON.stringify({ error: err.message }), { status: 500 });
  }
});