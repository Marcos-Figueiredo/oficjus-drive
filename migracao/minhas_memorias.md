# 🧠 MINHAS MEMÓRIAS — OficJus Drive

## 📋 Escopo deste documento

Tudo que aprendi, construímos, erramos e acertamos durante o desenvolvimento do OficJus Drive. Este documento serve como **bagagem cultural** para o novo projeto Android nativo.

---

## 1. ORIGEM DO PROJETO

### 1.1 Timeline

| Data | Evento |
|------|--------|
| ~Jan/2026 | Projeto OficJus Enterprise nasce (React + Supabase + OCR) |
| ~Jul/2026 | Decisão de criar o **Navega Fácil** como fork stand-alone |
| 31/Jul/2026 | Primeira sessão de refatoração massiva do Drive |
| 01/Ago/2026 | OCR removido, CEP+Número implementado, Waze integrado |
| 02/Ago/2026 | Geocodificação, fallbacks, IndexedDB, bolha flutuante web |
| 03/Ago/2026 | CNEFE importado, campo único, bolha nativa, APK, **decisão de migrar para nativo** |

### 1.2 O que era vs O que se tornou

| Aspecto | Original (fork) | Atual |
|---------|----------------|-------|
| **Nome** | OficJus Drive (herdado) | OficJus Drive |
| **Propósito** | Complemento do sistema Enterprise | App stand-alone de navegação GPS |
| **Autenticação** | Obrigatória (Supabase) | Nenhuma (modo Drive) |
| **Armazenamento** | Supabase (nuvem) | IndexedDB (local) |
| **Entrada de dados** | OCR de foto | CEP + Número ou autocomplete |
| **Geocodificação** | Nominatim + Google | CNEFE (IBGE) |
| **Navegação** | Mapa embarcado (Leaflet) | Waze externo |
| **Tamanho APK** | ~66MB (com OCR) | ~57MB (sem OCR, mas com React) |

### 1.3 Dual Purpose

O app sempre foi pensado para **dois modos de operação**:
- **Drive (stand-alone):** inserção manual, dados locais, sem login
- **Enterprise (integrado):** importa mandados do Supabase, registra visitas detalhadas

Na prática, o modo Enterprise é **código legado** do fork que nunca chegamos a usar no novo contexto. O foco sempre foi o modo Drive.

---

## 2. PROBLEMAS QUE ENFRENTAMOS

### 2.1 Problemas Resolvidos

#### 🏆 OCR / Tesseract.js
- **Sintoma:** Tesseract.js congelava o Android WebView, processamento demorado
- **Solução:** Removido completamente. Substituído por entrada manual de CEP + Número
- **Foi a decisão mais acertada do projeto**

#### 🏆 Geocodificação — Nominatim
- **Sintoma:** Nominatim bloqueava chamadas diretas do frontend (CORS/User-Agent)
- **Solução:** Edge function no Supabase faz o proxy → resolveu CORS
- **Depois:** CNEFE substituiu Nominatim para 95% dos casos

#### 🏆 Geocodificação — Google Maps
- **Sintoma:** Chave API expirada / REQUEST_DENIED
- **Tentativa:** Criar chave nova, habilitar Geocoding API
- **Resultado:** Chave continuou expirada (billing não ativado)
- **Solução final:** CNEFE tornou o Google desnecessário

#### 🏆 Waze em loop
- **Sintoma:** Waze abria repetidamente por causa do polling
- **Solução:** useRef `wazeOpened` para abrir apenas uma vez

#### 🏆 Dados fantasmas (Rua Sartre)
- **Sintoma:** Endereço excluído reaparecia
- **Causa:** Dados em dois lugares (Supabase + IndexedDB)
- **Solução:** Unificar no IndexedDB para modo Drive

#### 🏆 Campo único
- **Sintoma:** Dois modais separados (CEP + Número / Busca 🔍)
- **Solução:** Um campo inteligente que detecta se é CEP+número ou logradouro
- **UX final:** `35700388 500` → ViaCEP | `R OVIDIO` → autocomplete CNEFE → pede número

### 2.2 Problemas NÃO Resolvidos (levar para o nativo)

#### ❌ Bolha flutuante nativa
- **O que tentamos:**
  1. Plugin Capacitor `BolhaFlutuante` → não registrava
  2. `MainActivity.registerPlugin()` → ordem errada do lifecycle
  3. `initialPlugins.add()` → Capacitor sobrescreve no sync
  4. `JavascriptInterface` direto (`AndroidBolha`) → timing do bridge
- **Causa raiz:** O Capacitor WebView dificulta a comunicação Java ↔ JS
- **Solução no nativo:** WindowManager puro, sem camada intermediária. **No nativo isso é trivial.**

#### ❌ APK grande (57MB)
- React + Capacitor + plugins ocupam ~40MB só de framework
- No nativo, o APK deve cair para ~8-12MB

#### ❌ Dependências legadas
- `@capacitor-mlkit/text-recognition` (OCR não usado)
- `@capgo/background-geolocation` (não implementado)
- `tesseract.js` (removido mas ainda em dependências)
- várias outras

---

## 3. O QUE FUNCIONA BEM (manter no nativo)

### ✅ Campo único inteligente
```
Digite CEP + número (ex: 35700388 500) ou o nome do logradouro para buscar
```
- Se for dígitos + espaço + dígitos → busca ViaCEP
- Se tiver letras → autocomplete no CNEFE (logradouro_completo)
- Selecionou na lista → abre formulário pedindo o número
- Tudo num campo só, sem botão 🔍

### ✅ CNEFE (IBGE) como fonte de coordenadas
- 3.987.864 endereços únicos de MG importados
- Coluna virtual `logradouro_completo` com índice trigram (busca fuzzy)
- Interpolação de números inexistentes (ex: número 124 não existe → calcula entre 115 e 99)
- Consulta via REST API do Supabase

### ✅ Edge function geocode
- Pipeline: CNEFE → Nominatim → CEP fallback
- Chaves de API no servidor (nunca no cliente)
- Deployada em `https://weaqkaaqalvpbxkxrfee.supabase.co/functions/v1/geocode`

### ✅ Fluxo de otimização TSP
- Nearest-neighbor com Haversine
- Fallback para ordem original se não tiver coordenadas suficientes

### ✅ Abertura do Waze
- `waze://?ll=lat,lng&navigate=yes`
- Coordenadas exatas do CNEFE ou interpoladas

---

## 4. ARQUIVOS IMPORTANTES (referência)

### 4.1 Código React (legado)

| Arquivo | O que faz | Relevância |
|---------|-----------|------------|
| `src/pages/RouteBuildPage.js` | **O coração do app** — campo único, otimização, salvamento | ⭐⭐⭐ |
| `src/hooks/useBolhaFlutuante.js` | Hook da bolha via JavascriptInterface | ⭐⭐⭐ |
| `src/services/cnefeService.js` | Busca no CNEFE (logradouro_completo) | ⭐⭐⭐ |
| `src/services/supabaseGeocodeService.js` | Cliente da edge function | ⭐⭐ |
| `src/pages/NavigationPage.js` | Navegação + registro de visitas | ⭐⭐⭐ |
| `src/components/DriveOverlay.js` | Bolha web (não nativa) | ⭐ |
| `src/components/PermissionGate.js` | Tela de permissões primeira execução | ⭐⭐ |

### 4.2 Backend / Infraestrutura

| Arquivo | O que faz | Relevância |
|---------|-----------|------------|
| `supabase/functions/geocode/index.ts` | Edge function de geocodificação | ⭐⭐⭐ |
| `scripts/importar_cnefe.js` | Script de importação CNEFE | ⭐⭐⭐ |
| `scripts/recriar_cnefe.js` | Deduplicação e recriação | ⭐⭐⭐ |
| `scripts/criar_coluna_busca.js` | Coluna virtual + índice trigram | ⭐⭐ |

### 4.3 Android (plugin nativo — referência)

| Arquivo | O que faz | Relevância |
|---------|-----------|------------|
| `android/.../MainActivity.java` | JavascriptInterface + WindowManager | ⭐⭐⭐ |
| `android/.../bolha_flutuante_layout.xml` | Layout da bolha (◀ ☰ ▶) | ⭐⭐⭐ |
| `android/.../bf_fundo.xml` | Fundo semi-transparente | ⭐ |
| `android/.../bf_botao.xml` | Botões arredondados | ⭐ |
| `android/.../AndroidManifest.xml` | Permissões (SYSTEM_ALERT_WINDOW, etc) | ⭐⭐⭐ |

---

## 5. CONFIGURAÇÕES E CREDENCIAIS

### 5.1 Supabase (projeto: justica)

| Atributo | Valor |
|----------|-------|
| **URL** | `https://weaqkaaqalvpbxkxrfee.supabase.co` |
| **Anon Key** | `sb_publishable_K1A9Oo6uXRAmIXDATMThEw_jDy1fLAJ` |
| **Service Role** | (no .env, não expor) |
| **Projeto CLI** | `weaqkaaqalvpbxkxrfee` |
| **Plano** | Free (500MB) — **extrapolado para 1GB** |
| **DB String** | `postgresql://postgres:Senha14498620%40@db.weaqkaaqalvpbxkxrfee.supabase.co:5432/postgres` |

### 5.2 Google (NÃO FUNCIONA — chave expirada/billing)

| Atributo | Valor |
|----------|-------|
| **Chave** | `AIzaSyA2CflB0NlYvoas6zw9IK0AAxWQ4VIbs0o` |
| **Status** | `REQUEST_DENIED` — "API key is expired" |
| **Solução** | Precisa criar chave nova OU ativar billing no Google Cloud |

### 5.3 Edge Function

| Atributo | Valor |
|----------|-------|
| **Nome** | `geocode` |
| **URL** | `https://weaqkaaqalvpbxkxrfee.supabase.co/functions/v1/geocode` |
| **Segredos** | `GOOGLE_GEOCODE_API_KEY` (setado, mas chave expirada) |

---

## 6. TABELAS NO SUPABASE

### 6.1 `cnefe_enderecos` (⭐ principal)

```sql
CREATE TABLE cnefe_enderecos (
  id BIGSERIAL PRIMARY KEY,
  cep VARCHAR(8) NOT NULL,
  logradouro_tipo VARCHAR(50),       -- RUA, AV, PRACA
  logradouro_titulo VARCHAR(100),     -- SAO, NOSSA SENHORA
  logradouro_nome VARCHAR(200),       -- MIRIAN, FRANCISCO RAMOS
  numero VARCHAR(20) NOT NULL,
  bairro VARCHAR(200),
  cidade VARCHAR(100) NOT NULL,
  estado VARCHAR(2) NOT NULL,         -- código UF: '31' = MG
  latitude NUMERIC(10,7),
  longitude NUMERIC(10,7),
  logradouro_completo VARCHAR(300) GENERATED ALWAYS AS (TRIM(...)) STORED
);
```

**Índices:**
- `idx_cnefe_cep` (cep)
- `idx_cnefe_cep_numero` (cep, numero)
- `idx_cnefe_bairro` (bairro)
- `idx_cnefe_log_nome_trgm` (logradouro_nome gin_trgm_ops) ← busca fuzzy
- `idx_cnefe_log_completo_trgm` (logradouro_completo gin_trgm_ops) ← autocomplete

**⚠️ RLS desabilitado** — necessário para a chave anônima consultar.

### 6.2 `municipios`

```sql
CREATE TABLE municipios (
  codigo VARCHAR(7) PRIMARY KEY,    -- código IBGE
  uf VARCHAR(2) NOT NULL,           -- '31' = MG
  nome VARCHAR(100) NOT NULL        -- 'SETE LAGOAS'
);
```

**⚠️ Observação:** O CNEFE usa `COD_UF` como número ('31' = MG), não sigla. A tabela `municipios` tem `codigo` (código IBGE) e `uf` (código UF numérico).

### 6.3 `temp_municipios` (temporária, pode deletar)

Usada para importar o CSV de municípios.

---

## 7. LIÇÕES APRENDIDAS

### 7.1 Técnicas

| Lição | Detalhe |
|-------|---------|
| **React + Capacitor não é para apps com recursos nativos** | Bolha flutuante, overlay, GPS background — tudo é mais difícil no WebView |
| **CNEFE do IBGE é ouro** | 3.9M endereços de MG com coordenadas exatas, grátis, sem API |
| **Índice trigram salva busca textual** | Sem ele, `ilike.*termo*` em 3.9M registros é inviável |
| **COPY é muito mais rápido que REST** | 12M linhas em 72s via COPY vs ~16min via REST API |
| **Deduplicação no Node.js é mais rápida que no SQL** | `DISTINCT ON` em 12M registros timeout no Supabase |
| **JavascriptInterface precisa de timing** | `bridge.getWebView()` só funciona depois do `super.onCreate()` |
| **Capacitor 8 não descobre plugins locais** | Só plugins npm. Plugin local precisa de `initialPlugins.add()` |

### 7.2 De Produto

| Lição | Detalhe |
|-------|---------|
| **Campo único é a melhor UX** | Um campo que faz tudo elimina confusão |
| **CEP + Número é o suficiente** | Não precisa de OCR, foto, voz — CEP resolve |
| **Waze é o melhor navegador** | Não precisa de mapa embarcado |
| **Bolha flutuante é essencial** | O oficial precisa ver a distância e botões de ação |
| **Modo Enterprise é legado** | O fork nunca foi usado de verdade no novo contexto |

### 7.3 Armadilhas Evitadas

- ❌ **Não commitar sem autorização** — regra do Marcos
- ❌ **Não expor chaves de API no frontend** — edge function como proxy
- ❌ **Não misturar dados** — Drive no IndexedDB, Enterprise no Supabase
- ❌ **Não confiar 100% em Nominatim** — ruas novas não existem no OSM

---

## 8. PENDÊNCIAS PARA O NATIVO

| Item | Prioridade | Observação |
|------|-----------|------------|
| **Bolha flutuante nativa** | 🔴 Crítica | WindowManager puro, sem Capacitor |
| **Campo único** | 🔴 Crítica | UX já validada |
| **CNEFE via Retrofit** | 🔴 Crítica | Substitui Nominatim + Google |
| **Interpolação de números** | 🟡 Média | Lógica pronta no SQL |
| **Waze intent** | 🟡 Média | `waze://?ll=lat,lng&navigate=yes` |
| **TSP optimization** | 🟡 Média | Nearest-neighbor Haversine |
| **PermissionGate** | 🟢 Baixa | Tela de primeira execução |
| **Modo Enterprise** | 🟢 Baixa | Só quando integrar com o sistema principal |
| **Upgrade Supabase Pro** | 🟢 Baixa | Aguardar e-mail de extrapolação |

---

## 9. DECISÕES ARQUITETURAIS PARA O NATIVO

| Decisão | Por quê |
|---------|---------|
| **Kotlin + Jetpack Compose** | Moderno, conciso, oficial |
| **MVVM + Clean Architecture** | Separação de responsabilidades, testável |
| **Hilt para DI** | Troca de implementações (Drive vs Enterprise) |
| **Room para DB local** | Cache de endereços + visitas do modo Drive |
| **Retrofit para rede** | Comunicação com Supabase + edge function |
| **WindowManager para bolha** | TYPE_APPLICATION_OVERLAY, nativo, sem WebView |
| **FusedLocationProvider** | GPS preciso e econômico |
| **Coroutines + Flow** | Async reativo |
| **ProGuard + SSL Pinning** | Segurança |
| **Edge function como proxy** | Chaves nunca no cliente |

---

## 10. FRASES E CONCEITOS DO MARCOS

- *"Nada de fotografar endereços. Vamos trabalhar única e exclusivamente com CEP + número do imóvel."*
- *"Nossa lista de visitas fica em background durante a execução da rota?"*
- *"Não sei o CEP"* — botão que levou ao autocomplete
- *"Determinados tipos de chamada não são aceitas pelo Nominatim"* — edge function
- *"A bolha deve flutuar sobre o Waze"* — decisão crítica
- *"A chave do Google não pode ser exposta"* — edge function
- *"Separa o app em um projeto totalmente novo"* — decisão de migrar para nativo
- *"Está ficando muuuuito bom isto daqui"* — quando o campo único funcionou
- *"Acho que nunca vi navegador com tamanha facilidade de inserção de dados"* — validação da UX

---

## 11. INFRAESTRUTURA DO SUPABASE

### Projetos

| Projeto | ID | Status | Finalidade |
|---------|-----|--------|------------|
| **justica** | `weaqkaaqalvpbxkxrfee` | ACTIVE_HEALTHY | OficJus Enterprise + Drive |
| **pethouse** | `pyqdljlhezrsdioybqaq` | INACTIVE | Projeto antigo, pausado |
| **navega-facil** | `npwisdiywqeqymwdpdpc` | ACTIVE_HEALTHY | Navega Fácil (não usado) |

### Tabelas do sistema Enterprise (não tocar)

Mandados, mandado_tentativas, profiles, rota, certidoes, adm_*, cnj_* — são do sistema principal.

### Tabelas do Drive

`cnefe_enderecos`, `municipios` — criadas pelo Drive.

---

## 12. SCRIPTS ÚTEIS

### Importar CNEFE
```
c:\project-oficjus\scripts\importar_cnefe.js
```
Lê CSV do IBGE, limpa coordenadas, faz JOIN com municipios, importa para Supabase.

### Recriar tabela deduplicada
```
c:\project-oficjus\scripts\recriar_cnefe.js
```
Remove duplicatas (apartamentos, sublocações), recria tabela com índices.

### Criar coluna de busca
```
c:\project-oficjus\scripts\criar_coluna_busca.js
```
Adiciona coluna virtual `logradouro_completo` + índice trigram.

### Edge function deploy
```bash
cd c:\project-oficjus
npx supabase functions deploy geocode
```

---

## 13. ADVERTÊNCIAS FINAIS

⚠️ **Não commitar sem o Marcos pedir** — regra número 1

⚠️ **A chave do Google está expirada** — Google Geocoding não funciona. CNEFE substitui.

⚠️ **O Supabase Free tem 500MB** — estamos com 1GB. Vai chegar e-mail de cobrança.

⚠️ **O `|| true` no modo Drive** precisa ser removido quando o app for para produção.

⚠️ **O OCR/Tesseract ainda está nas dependências** — não usar, será removido no nativo.

⚠️ **A senha do banco tem `@`** — precisa usar `%40` na URL de conexão.

---

*Documento gerado em 03/08/2026 como bagagem cultural para o novo projeto Android nativo.*
*"OficJus Drive: nasceu como fork, vive como nativo."* 🚀