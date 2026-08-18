// ============================================================
// OficJus Drive — Abstração do Gateway de Pagamento
// ============================================================

/**
 * Abstração do gateway de pagamento.
 * Atualmente preparado para Asaas, mas pode ser trocado.
 */

class PaymentGateway {
  constructor(config) {
    this.apiKey = config.apiKey || '';
    this.environment = config.environment || 'sandbox';
    this.baseUrl = this.environment === 'production'
      ? 'https://api.asaas.com/v3'
      : 'https://sandbox.asaas.com/api/v3';
  }

  async criarCliente(dados) {
    const response = await fetch(`${this.baseUrl}/customers`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'access_token': this.apiKey,
      },
      body: JSON.stringify({
        name: dados.nome,
        email: dados.email,
        cpfCnpj: dados.cpfCnpj,
        phone: dados.telefone,
        notificationDisabled: false,
      }),
    });
    return response.json();
  }

  async criarAssinatura(clienteId, plano) {
    const response = await fetch(`${this.baseUrl}/subscriptions`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'access_token': this.apiKey,
      },
      body: JSON.stringify({
        customer: clienteId,
        billingType: 'CREDIT_CARD',
        value: plano.precoMensal,
        nextDueDate: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
        cycle: 'MONTHLY',
        description: `OficJus Drive - ${plano.nome}`,
      }),
    });
    return response.json();
  }

  async criarCobrancaUnica(clienteId, valor, descricao) {
    const response = await fetch(`${this.baseUrl}/payments`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'access_token': this.apiKey,
      },
      body: JSON.stringify({
        customer: clienteId,
        billingType: 'CREDIT_CARD',
        value: valor,
        dueDate: new Date(Date.now() + 3 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
        description: descricao,
      }),
    });
    return response.json();
  }

  async consultarStatus(pagamentoId) {
    const response = await fetch(`${this.baseUrl}/payments/${pagamentoId}`, {
      headers: { 'access_token': this.apiKey },
    });
    return response.json();
  }
}

export { PaymentGateway };