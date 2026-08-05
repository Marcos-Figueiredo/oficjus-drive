# OficJus Drive

App complementar do OficJus para execucao de rota em central multimidia Android.

## Objetivo

Este projeto deve permanecer isolado do sistema principal. Ele usa o mesmo backend e a mesma autenticacao, mas possui interface e ciclo de navegacao proprios.

## Escopo inicial

- login
- rota ativa
- navegacao operacional
- acoes rapidas de diligencia
- pausa e retomada
- resumo final

## Estrutura

```text
drive/
  public/
  src/
    components/
    context/
    lib/
    pages/
    services/
    App.css
    App.js
    index.css
    index.js
  package.json
  capacitor.config.ts
```

## Observacoes

- Este projeto nao deve importar componentes do `frontend/`
- Compartilhamento futuro deve ocorrer por pacotes reutilizaveis, nao por acoplamento direto
- O backend e o Supabase permanecem os mesmos do OficJus principal

## Caminho recomendado

Este subprojeto deve evoluir como `React + Capacitor`, para gerar APK Android e aproveitar melhor:

- GPS nativo
- wake lock
- audio
- ciclo de vida do app
- instalacao em central multimidia

## Proximos passos tecnicos

1. copiar `.env.example` para `.env`
2. instalar dependencias do `drive/`
3. rodar o projeto web localmente
4. inicializar/sincronizar o Android com Capacitor
5. abrir no Android Studio

## Comandos previstos

```bash
npm install
npm run build
npm run cap:sync
npm run cap:open
```

## Observacao importante

O `drive/` esta preparado para Capacitor, mas a plataforma Android ainda nao foi gerada neste momento. Isso sera feito quando iniciarmos a fase de instalacao do subprojeto.
