# OficJus Drive — Backend

Sistema independente de backend para o OficJus Drive.
Gerencia cadastro, autenticação, licenciamento e pagamentos.

## Estrutura

```
backend/
├── README.md
├── api/                    # API endpoints (Edge Functions Supabase)
│   ├── register-account/   # Cria conta + trial
│   ├── check-license/      # Verifica status da assinatura
│   └── webhook/            # Webhook de pagamento (Asaas)
├── licenciamento/          # Lógica de licenciamento
│   ├── rules.js            # Regras de validação
│   └── plans.js            # Planos e preços
├── pagamentos/             # Integração de pagamento
│   ├── asaas.js            # API Asaas
│   └── gateway.js          # Abstração do gateway
├── supabase/
│   └── migrations/         # Migrations SQL específicas do Drive
├── web/                    # Páginas web (login, cadastro, etc.)
│   ├── cadastro.html
│   ├── cadastro.js
│   ├── login.html
│   ├── auth.js
│   ├── supabase-init.js
│   └── contrato.html
└── config.js               # Configurações centralizadas
```

## Fluxo de Licenciamento

1. Usuário se cadastra → trial de 7 dias
2. App verifica licença no login (check-license)
3. Se trial expirou → bloqueia até pagar
4. Pagamento via Asaas → webhook atualiza status
5. Se pagamento OK → assinatura ativa
6. Se inadimplente → bloqueia após X dias

## Tecnologias

- Supabase (Auth + PostgreSQL + Edge Functions)
- Asaas (gateway de pagamento)
- Vanilla JS (páginas web)