// ============================================================
// OficJus Drive — Configurações Centralizadas
// ============================================================

const CONFIG = {
  // Supabase
  supabase: {
    url: 'https://weaqkaaqalvpbxkxrfee.supabase.co',
    anonKey: 'sb_publishable_K1A9Oo6uXRAmIXDATMThEw_jDy1fLAJ',
  },

  // Planos
  planos: {
    drive: {
      nome: 'OficJus Drive',
      precoMensal: 29.90,
      precoAnual: 299.00,
      trialDias: 7,
    },
  },

  // Licenciamento
  licenciamento: {
    trialDias: 7,
    diasGracePeriod: 5, // dias após expirar antes de bloquear
    verificarAoCadaLogin: true,
  },

  // Asaas (gateway de pagamento)
  asaas: {
    // Chaves serão configuradas quando a conta PJ for criada
    apiKey: '',
    environment: 'sandbox', // sandbox | production
  },

  // App
  app: {
    nome: 'OficJus Drive',
    versao: '1.0.0',
    emailContato: 'contato@oficjus.com.br',
    deepLink: 'oficjus-drive://open',
  },
};

// Node.js / Edge Function
if (typeof module !== 'undefined' && module.exports) {
  module.exports = { CONFIG };
}