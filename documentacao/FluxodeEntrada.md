# Fluxo de Entrada no Sistema — OficJus Drive

## Visão Geral

O fluxo de entrada no sistema compreende desde o login do usuário até a chegada à tela principal de construção da rota. Este fluxo envolve: autenticação, verificação de requisitos do dispositivo, autorização de permissões, sincronização da base de endereços e carregamento do estado inicial.

---

## 1. Navegação entre Telas

**Arquivo:** `ui/navigation/DriveNavGraph.kt`

```
Login → WazeGate → RouteBuild (construção da rota)
```

A navegação é controlada pelo `NavHost` do Jetpack Compose Navigation:

```kotlin
startDestination = Screen.Login.route

composable(Screen.Login.route) → LoginScreen
  onLoginSuccess → navigate(Screen.WazeGate.route)

composable(Screen.WazeGate.route) → WazeGateScreen
  onWazeConfirmado → navigate(Screen.RouteBuild.route)

composable(Screen.RouteBuild.route) → RouteBuildScreen
  onRotaConfirmada → navigate(Screen.RouteActive.createRoute(rotaId))
```

---

## 2. Tela de Login

**Arquivo:** `ui/login/LoginScreen.kt` + `ui/login/LoginViewModel.kt`

### 2.1 Formulário

A tela de login possui:
- Campo de **email** (teclado tipo e-mail, ação "Next")
- Campo de **senha** (com `PasswordVisualTransformation`, ação "Done")
- Botão **"Entrar"** com indicador de carregamento
- Exibição de mensagem de erro

### 2.2 Validações

```kotlin
fun login() {
    if (email.isBlank()) → erro "Digite seu email"
    if (password.isBlank()) → erro "Digite sua senha"
}
```

### 2.3 Autenticação

**Arquivo:** `data/repository/AuthRepositoryImpl.kt`

```kotlin
suspend fun login(email: String, password: String): Result<Usuario> {
    // 1. Chama Supabase Auth REST (/auth/v1/token)
    val response = authApi.signIn(SupabaseSignInRequest(email, password))

    // 2. Salva tokens no SessionManager
    sessionManager.accessToken = response.accessToken
    sessionManager.refreshToken = response.refreshToken
    sessionManager.userId = user.id
    sessionManager.userEmail = user.email

    // 3. Busca perfil do usuário na tabela profiles
    buscarPerfil(accessToken)  // ← carrega estado, cidade, CNJ, usage

    // 4. Retorna Usuario com dados do perfil
    return Usuario(
        id = user.id, email = user.email,
        estado = sessionManager.userEstado,
        cidade = sessionManager.userCidade,
        comarcaId = sessionManager.userComarcaId,
        segmentoId = sessionManager.userSegmentoId,
        tribunalId = sessionManager.userTribunalId,
        usage = sessionManager.userUsage
    )
}
```

### 2.4 Sessão persistente

**Arquivo:** `data/local/SessionManager.kt`

```kotlin
class SessionManager {
    // Armazenamento criptografado (EncryptedSharedPreferences)
    var accessToken: String?     // token JWT
    var refreshToken: String?    // refresh token
    var userId: String?          // UUID do usuário no Supabase Auth
    var userEmail: String?
    var userEstado: String?      // UF (ex: "MG")
    var userCidade: String?      // Cidade (ex: "SETE LAGOAS")
    var userComarcaId: String?   // Código CNJ da comarca
    var userSegmentoId: String?  // Código CNJ do segmento
    var userTribunalId: String?  // Código CNJ do tribunal
    var userUsage: String?       // "comarca" | "estado"
}
```

### 2.5 Sessão ativa (auto-login)

No `init {}` do `LoginViewModel`, verifica se já existe sessão válida:

```kotlin
init {
    val session = authRepository.getSession()
    if (session != null) {
        _state.value = _state.value.copy(isLoggedIn = true, usuario = session)
    }
}
```

Se sim, o login é pulado e o app navega direto para o `WazeGate`.

---

## 3. Tela de Verificação de Requisitos (WazeGate)

**Arquivo:** `ui/wazegate/WazeGateScreen.kt`

### 3.1 Requisitos verificados

| # | Requisito | Método de verificação | Android |
|---|---|---|---|
| 1 | **Waze instalado** | `WazeNavigator.isWazeInstalled()` — verifica se `waze://` tem app registrado | Todos |
| 2 | **Localização (GPS)** | `ContextCompat.checkSelfPermission(ACCESS_FINE_LOCATION)` | 6+ |
| 3 | **Notificações** | `checkSelfPermission(POST_NOTIFICATIONS)` | 13+ |
| 4 | **Sobreposição (bolha)** | `Settings.canDrawOverlays(context)` | 6+ |
| 5 | **Microfone (voz)** | `checkSelfPermission(RECORD_AUDIO)` | 6+ |

### 3.2 Interface

```
🛡️ OficJus Drive

Para usar o app é necessário:

✅ Waze instalado
⬜ Permissão de localização (GPS)
✅ Permissão de notificações
⬜ Permissão de sobreposição (bolha)
✅ Permissão de microfone (ditado por voz)

Resolva os itens pendentes e volte ao app.

[Instalar Waze]        ← só aparece se Waze não estiver instalado
[Permitir localização] ← só aparece se permissão negada
[Permitir notificações]
[Permitir sobreposição]
[Permitir microfone]
```

### 3.3 Fluxo de autorização

Cada item pendente tem um botão que dispara a solicitação de permissão:

```kotlin
// Localização
permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)

// Notificação
notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)

// Sobreposição (bolha) — abre Settings
Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))

// Microfone
audioLauncher.launch(Manifest.permission.RECORD_AUDIO)

// Waze — abre Play Store
Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.waze"))
```

### 3.4 Auto-avanço

Quando **todos os requisitos** estão ok, o app avança automaticamente:

```kotlin
// Monitora o ciclo de vida (voltar de Settings, instalar Waze e voltar)
DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) {
            // Re-verifica todas as permissões
            if (tudoOk) onWazeConfirmado()  // ← navega para RouteBuild
        }
    }
}

// Também avança imediatamente se tudo já estiver ok
LaunchedEffect(requisitos) {
    if (tudoOk) onWazeConfirmado()
}
```

---

## 4. Carregamento da Tela de Construção (RouteBuild)

**Arquivo:** `ui/routebuild/RouteBuildViewModel.kt` — `init {}`

Ao entrar na tela de construção, o app executa em sequência:

### 4.1 Carregamento de remanescentes

```kotlin
val remanescentes = enderecoRepository.getRemanescentes()
if (remanescentes.isNotEmpty()) {
    val renumerados = remanescentes.mapIndexed { index, e ->
        e.copy(referencia = index + 1, ordem = index + 1)
    }
    _state.value = _state.value.copy(paradas = renumerados)
    enderecoRepository.limparRemanescentes()
    mostrarMensagem("${remanescentes.size} remanescente(s) da rota anterior")
}
```

### 4.2 Carregamento do perfil do usuário

```kotlin
var session = authRepository.getSession()
usuarioEstado = session?.estado
usuarioCidade = session?.cidade

// Refresh para garantir dados atualizados
authRepository.refreshProfile()
session = authRepository.getSession()
usuarioEstado = session?.estado
usuarioCidade = session?.cidade

// Fallback
if (usuarioCidade == null) usuarioCidade = "SETE LAGOAS"
if (usuarioEstado == null) usuarioEstado = "MG"

// Dados da comarca e tribunal
val comarcaCod = session?.comarcaId ?: ""
val usage = session?.usage ?: "comarca"
val tribunalFolder = when (session?.tribunalId) {
    "06" -> "TRF6"
    "13" -> "TJMG"
    else -> "TJMG"
}
```

### 4.3 Sincronização da base CNEFE

```kotlin
if (cnefeUnificadaSync.precisaSincronizar()) {
    // usage=estado → baixa MG inteiro; usage=comarca → baixa só a comarca
    val comarcaParam = if (usage == "comarca") comarca else null
    val texto = if (comarcaParam != null) "Baixando base CNEFE da comarca..."
                 else "Baixando base CNEFE (43 MB)..."

    _state.value = _state.value.copy(syncProgresso = texto, syncProgressoPorcentagem = 0.1f)
    cnefeUnificadaSync.sincronizar(estado, comarcaParam, tribunalFolder)

    val totalLog = cnefeUnificadaSync.contarLogradouros()
    val totalNum = cnefeUnificadaSync.contarNumeros()
    mostrarMensagem("Base CNEFE: $totalLog logradouros, $totalNum números")
} else {
    // Cache já existe, só verifica tamanho
    val totalLog = cnefeUnificadaSync.contarLogradouros()
    val totalNum = cnefeUnificadaSync.contarNumeros()
    if (totalLog > 0) {
        mostrarMensagem("Cache CNEFE: $totalLog logradouros, $totalNum números")
    }
}
```

### 4.4 Tela pronta para uso

Após a sincronização, o app exibe:

- **Campo de busca inteligente** (`SmartSearchField`) — busca por logradouro ou CEP
- **Entrada por voz** (`VoiceInputButton`) — ditado de endereços
- **Lista de paradas** (vazia ou com remanescentes)
- **Botão "Salvar Rota"** — quando houver paradas

---

## 5. Diagrama do Fluxo Completo

```
┌──────────┐
│  INÍCIO  │
└────┬─────┘
     │
     ▼
┌──────────┐     ┌──────────────┐
│  LOGIN   │────→│ Sessão ativa?│
└──────────┘     └──────┬───────┘
     │                   │
     │ sim               │ não
     │                   ▼
     │            ┌──────────────┐
     │            │ Tela de      │
     │            │ Login        │
     │            │ (email+senha)│
     │            └──────┬───────┘
     │                   │
     │                   ▼
     │            ┌──────────────┐
     │            │ Autenticar   │
     │            │ no Supabase  │
     │            └──────┬───────┘
     │                   │
     │                   ▼
     │            ┌──────────────┐
     │            │ Buscar       │
     │            │ Perfil (UF,  │
     │            │ cidade, CNJ) │
     │            └──────┬───────┘
     │                   │
     └──────┬────────────┘
            │
            ▼
     ┌──────────────┐
     │  WAZEGATE    │
     │  Verificar:  │
     │  • Waze      │
     │  • GPS       │
     │  • Notif.    │
     │  • Bolha     │
     │  • Microfone │
     └──────┬───────┘
            │
            ▼ (tudo ok)
     ┌──────────────┐
     │  ROUTEBUILD  │
     │  • Remanesc. │
     │  • Perfil    │
     │  • Sync CNEFE│
     │  • Pronto!   │
     └──────────────┘
```

---

## 6. Arquivos Envolvidos

| Arquivo | Função |
|---|---|
| `ui/navigation/DriveNavGraph.kt` | Navegação entre telas (Login → WazeGate → RouteBuild) |
| `ui/navigation/Screen.kt` | Definição das rotas |
| `ui/login/LoginScreen.kt` | UI da tela de login |
| `ui/login/LoginViewModel.kt` | Lógica de login e validação |
| `data/repository/AuthRepositoryImpl.kt` | Autenticação + busca de perfil + refresh |
| `data/local/SessionManager.kt` | Sessão criptografada (SharedPreferences) |
| `data/remote/SupabaseAuthApi.kt` | API REST do Supabase Auth |
| `ui/wazegate/WazeGateScreen.kt` | Verificação de requisitos e permissões |
| `domain/usecase/WazeNavigator.kt` | Verificação de Waze instalado |
| `ui/routebuild/RouteBuildViewModel.kt` | Inicialização da tela de construção |
| `data/local/CnefeUnificadaSync.kt` | Sincronização da base CNEFE |
| `domain/usecase/SessionTimeoutManager.kt` | Timeout de 5 minutos |

---

## ✅ Conclusão

O app **cumpre integralmente** todos os requisitos do fluxo de entrada:

| Requisito | Status |
|---|---|
| Tela de login com email e senha | ✅ |
| Validação de campos obrigatórios | ✅ |
| Autenticação via Supabase Auth REST | ✅ |
| Sessão persistente (auto-login) | ✅ |
| Busca de perfil (UF, cidade, CNJ, usage) | ✅ |
| Verificação de Waze instalado | ✅ |
| Solicitação de permissão de localização (GPS) | ✅ |
| Solicitação de permissão de notificações | ✅ |
| Solicitação de permissão de sobreposição (bolha) | ✅ |
| Solicitação de permissão de microfone (voz) | ✅ |
| Auto-avanço quando todos os requisitos são atendidos | ✅ |
| Carregamento de remanescentes da rota anterior | ✅ |
| Sincronização da base CNEFE (endereços e números) | ✅ |
| Timeout de 5 minutos (suspenso durante execução) | ✅ |
| Chegada à tela de construção pronta para uso | ✅ |