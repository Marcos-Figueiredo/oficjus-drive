// ============================================================
// OficJus Drive — Regras de Licenciamento
// ============================================================

/**
 * Verifica se o usuário tem licença válida para usar o app.
 * @param {Object} profile - Perfil do usuário (tabela profiles)
 * @returns {{ valido: boolean, motivo: string|null }}
 */
export function verificarLicenca(profile) {
  if (!profile) {
    return { valido: false, motivo: 'Usuário não encontrado' };
  }

  const status = profile.status_assinatura;

  // Ativo: liberado
  if (status === 'ativo') {
    return { valido: true, motivo: null };
  }

  // Vitalício: liberado para sempre
  if (status === 'vitalicio') {
    return { valido: true, motivo: null };
  }

  // Trial: verifica se expirou
  if (status === 'trial') {
    const trialFim = profile.trial_fim
      ? new Date(profile.trial_fim)
      : new Date(new Date(profile.trial_inicio).getTime() + 7 * 24 * 60 * 60 * 1000);

    if (new Date() <= trialFim) {
      return { valido: true, motivo: null };
    }

    // Trial expirou: verifica grace period
    const graceFim = new Date(trialFim.getTime() + 5 * 24 * 60 * 60 * 1000);
    if (new Date() <= graceFim) {
      return {
        valido: true,
        motivo: `Trial expirou em ${trialFim.toLocaleDateString('pt-BR')}. Assine para continuar.`,
      };
    }

    return {
      valido: false,
      motivo: 'Período de teste expirou. Assine o OficJus Drive para continuar usando.',
    };
  }

  // Inadimplente
  if (status === 'inadimplente') {
    return {
      valido: false,
      motivo: 'Assinatura pendente de pagamento. Regularize para continuar.',
    };
  }

  // Cancelado
  if (status === 'cancelado') {
    return {
      valido: false,
      motivo: 'Assinatura cancelada. Renove para continuar usando.',
    };
  }

  return { valido: false, motivo: 'Status de assinatura desconhecido.' };
}

/**
 * Retorna os dados do plano do usuário.
 */
export function getPlanoInfo(profile) {
  const plano = profile?.plano || 'drive';
  const planos = {
    drive: {
      nome: 'OficJus Drive',
      precoMensal: 29.90,
      precoAnual: 299.00,
      trialDias: 7,
    },
  };
  return planos[plano] || planos.drive;
}