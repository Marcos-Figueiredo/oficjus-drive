// ============================================================
// OficJus Drive — Planos e Preços
// ============================================================

export const PLANOS = {
  drive: {
    id: 'drive',
    nome: 'OficJus Drive',
    descricao: 'Navegador inteligente de rotas com bolha flutuante',
    recursos: [
      'Bolha flutuante sobre o Waze',
      'Rota dinâmica por GPS',
      '100% offline',
      'Base oficial IBGE (CNEFE)',
      'Entrada por voz',
      'Rota eterna (remanescentes)',
      'Alerta de duplicidade',
      'Suporte direto',
    ],
    preco: {
      mensal: 29.90,
      anual: 299.00, // ~24,92/mês
    },
    trial: {
      dias: 7,
      descricao: '7 dias grátis, sem compromisso',
    },
  },
};

/**
 * Retorna o preço formatado.
 */
export function formatarPreco(valor) {
  return `R$ ${valor.toFixed(2).replace('.', ',')}`;
}

/**
 * Calcula a data de fim do trial.
 */
export function calcularFimTrial(inicio = new Date()) {
  const fim = new Date(inicio);
  fim.setDate(fim.getDate() + 7);
  return fim;
}