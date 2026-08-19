# OficJus Drive — Landing Page

Landing page de alta conversão para o **OficJus Drive**, aplicativo Android nativo de otimização de rotas em campo 100% offline.

## Stack

- **Next.js 14** (App Router)
- **React 18** + TypeScript
- **Tailwind CSS 3**
- **Framer Motion 11**
- **Lucide React** (ícones)

## Identidade Visual

| Token            | Valor                          |
|------------------|--------------------------------|
| Background       | `#0B0F19`                      |
| Cyan elétrico    | `#00E5FF`                      |
| Lime neon        | `#A3E635`                      |
| Surface          | `#121826` / `#1A2235`          |
| Glass            | `rgba(18,24,38,0.65)` + blur   |

## Seções

1. **Header** — Logo + nav + CTA sticky
2. **Hero** — Headline, CTAs com pulse neon, badges de métricas
3. **Fluxo em 3 passos** — DIGITA → INICIA → BOLHA
4. **Bento Grid** — Features com simulação interativa da Bolha Flutuante
5. **Base CNEFE** — Contador animado 3,9M endereços
6. **Stack Técnica** — Painel estilo terminal
7. **Segurança** — RLS, auth, zero tracking
8. **Pricing + CTA** — Plano único + formulário de captura

## Como rodar

```bash
cd oficjus-drive
npm install
npm run dev
```

Abra [http://localhost:3000](http://localhost:3000).

## Componentes interativos

- **FloatingBubbleDemo**: mock de celular com mapa neon, bolha flutuante clicável (Confirmar / Pular / Lista de pendentes), barra de progresso e reordenação visual de pontos.
- **AnimatedCounter**: contador que sobe de 0 até 3,9M ao entrar no viewport.
- Micro-interações: glow nos cards Bento no hover, pulse no CTA principal.

## Acessibilidade

- Labels ARIA em botões interativos
- Focus rings visíveis
- Contraste adequado no dark mode
- Navegação por teclado no formulário e menu mobile
