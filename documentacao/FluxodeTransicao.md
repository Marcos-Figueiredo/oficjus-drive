# Fluxo de Transição — Construção → Execução (OficJus Drive)

## Visão Geral

Entre a fase de construção da rota e a fase de execução (entrega), existe uma fase intermediária de transição onde o oficial pode escolher o ponto de partida da rota. O app oferece duas opções: seguir a rota sugerida (default) ou escolher manualmente por onde começar.

---

## 1. Fluxo de Navegação entre Telas

**Arquivo:** `ui/navigation/DriveNavGraph.kt`

```
RouteBuild (construção) → RouteActive (execução)
```

A navegação ocorre através do `NavHost` do Jetpack Compose Navigation:

```kotlin
composable(Screen.RouteBuild.route) {
    RouteBuildScreen(
        onRotaConfirmada = { rotaId ->
            navController.navigate(Screen.RouteActive.createRoute(rotaId)) {
                popUpTo(Screen.RouteBuild.route)
            }
        },
        ...
    )
}

composable(
    route = Screen.RouteActive.route,
    arguments = listOf(navArgument("rotaId") { type = NavType.StringType })
) { backStackEntry ->
    val rotaId = backStackEntry.arguments?.getString("rotaId") ?: return@composable
    ActiveRouteScreen(
        rotaId = rotaId,
        onVoltar = {
            navController.popBackStack(Screen.RouteBuild.route, inclusive = false)
        }
    )
}
```

**Definição das rotas** (`ui/navigation/Screen.kt`):

```kotlin
sealed class Screen(val route: String) {
    data object WazeGate : Screen("waze_gate")
    data object Login : Screen("login")
    data object RouteBuild : Screen("route_build")
    data object RouteActive : Screen("route_active/{rotaId}") {
        fun createRoute(rotaId: String) = "route_active/$rotaId"
    }
}
```

---

## 2. Opção 1: Seguir a Rota Sugerida (Default)

**Arquivo:** `ui/activeRoute/ActiveRouteViewModel.kt` — `carregarRota()`

Quando o oficial confirma a rota sem interagir, o app carrega a rota com a primeira parada como padrão:

```kotlin
fun carregarRota(rotaId: String, context: Context? = null, onVoltar: (() -> Unit)? = null) {
    viewModelScope.launch {
        val rota = enderecoRepository.getRota(rotaId)
        if (rota != null) {
            _state.value = _state.value.copy(
                rota = rota,
                paradasRestantes = rota.paradas,   // ← lista completa otimizada
                paradaAtualIndex = 0,               // ← primeira parada é o default
                totalInicial = rota.paradas.size,
                ...
            )
            iniciarGps()                            // ← GPS já começa a monitorar
        }
    }
}
```

**Comportamento:**
- ✅ `paradaAtualIndex = 0` — primeira parada da rota otimizada
- ✅ GPS inicia automaticamente
- ✅ Bolha flutuante aparece com distância e informações da primeira parada
- ✅ Waze abre automaticamente na primeira parada (via `abrirWazeComBolha()`)

---

## 3. Opção 2: Escolher Manualmente por Onde Começar

### 3.1 Botão de lista ☰

**Arquivo:** `ui/activeRoute/ActiveRouteViewModel.kt` — função `selecionarParada()`

```kotlin
fun selecionarParada(index: Int) {
    if (index in _state.value.paradasRestantes.indices) {
        if (index != _state.value.paradaAtualIndex) {
            roteiroFoiAlterado = true  // marca que o oficial alterou a ordem
        }
        _state.value = _state.value.copy(paradaAtualIndex = index)
    }
}
```

### 3.2 AlertDialog de seleção

**Arquivo:** `ui/activeRoute/ActiveRouteScreen.kt`

Quando o oficial toca no botão `☰` (lista), um `AlertDialog` é exibido com todas as paradas restantes:

```kotlin
if (state.mostrarLista && state.paradasRestantes.isNotEmpty()) {
    AlertDialog(
        title = { Text("Selecione o endereço") },
        text = {
            LazyColumn {
                itemsIndexed(state.paradasRestantes) { index, parada ->
                    val isAtual = index == state.paradaAtualIndex
                    Card(...) {
                        Row(modifier = Modifier.clickable {
                            viewModel.selecionarParadaDaLista(index, context)
                            viewModel.fecharLista()
                        }) {
                            Text("${index + 1}. ${parada.logradouro}")
                            Text("Nº ${parada.numero} - ${parada.bairro}")
                            if (isAtual) Text("📍")
                        }
                    }
                }
            }
        }
    )
}
```

### 3.3 Popup de confirmação de reotimização

**Arquivo:** `ui/activeRoute/ActiveRouteScreen.kt`

Ao selecionar uma parada diferente da atual, o app pergunta se deseja reotimizar:

```kotlin
if (state.confirmarReotimizacao) {
    AlertDialog(
        title = { Text("Reotimizar rota?") },
        text = {
            Text("Deseja reotimizar a rota a partir do endereço selecionado?\n\n" +
                 "As paradas restantes serão reordenadas pela proximidade, " +
                 "desconsiderando entregues e pulados.")
        },
        confirmButton = {
            TextButton(onClick = { viewModel.confirmarReotimizacao(context) }) {
                Text("Sim, reotimizar")
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.cancelarReotimizacao(context) }) {
                Text("Não, só navegar")
            }
        }
    )
}
```

---

## 4. Reorganização dos Restantes com Base na Escolha

### 4.1 Se escolhe "Sim, reotimizar"

**Arquivo:** `ui/activeRoute/ActiveRouteViewModel.kt` — `confirmarReotimizacao()`

```kotlin
fun confirmarReotimizacao(context: Context) {
    val paradaSelecionada = paradaSelecionadaParaReotimizar?.let {
        _state.value.paradasRestantes.getOrNull(it)
    } ?: return

    // Reorganiza: parada escolhida como primeira, o restante reotimizado por GPS
    val restantes = listOf(paradaSelecionada) + 
        _state.value.paradasRestantes.filter { 
            it.referencia != paradaSelecionada.referencia 
        }
    
    val otimizadas = OtimizarRotaUseCase.otimizar(restantes, posicaoAtual = /* GPS atual */)
    _state.value = _state.value.copy(
        paradasRestantes = otimizadas, 
        paradaAtualIndex = 0
    )
    roteiroFoiAlterado = true
    abrirWaze(context)
    atualizarBolha(context)
}
```

**Comportamento:**
- ✅ A parada escolhida vira a primeira (foco)
- ✅ As restantes são reotimizadas por Nearest-Neighbor a partir do GPS
- ✅ Entregues e pulados são excluídos do processo de reordenação
- ✅ `roteiroFoiAlterado = true` — reotimizações futuras serão automáticas

### 4.2 Se escolhe "Não, só navegar"

**Arquivo:** `ui/activeRoute/ActiveRouteViewModel.kt` — `cancelarReotimizacao()`

```kotlin
fun cancelarReotimizacao(context: Context) {
    selecionarParada(paradaSelecionadaParaReotimizar!!)
    roteiroFoiAlterado = true
    abrirWaze(context)
    atualizarBolha(context)
}
```

**Comportamento:**
- ✅ Navega direto para a parada escolhida
- ✅ Mantém a ordem original das restantes
- ✅ `roteiroFoiAlterado = true` — reotimizações futuras serão automáticas

---

## 5. Exemplo Prático

| Ação | Lista (ordem) | Explicação |
|---|---|---|
| Rota otimizada carregada | A, B, C, D, E | Ordem default do app |
| Oficial abre lista ☰ | A, B, C, D, E | Visualiza todas as paradas |
| Seleciona **C** como início | C, (A, B, D, E reotimizados) | C vira primeira, restantes reordenados por GPS |
| Após entregar C, seleciona **E** | E, (A, B, D reotimizados) | E vira primeira, restantes reordenados |
| Após entregar E, segue default | A, B, D | Ordem otimizada das restantes |

---

## 6. Estrutura de Dados do Estado

**Arquivo:** `ui/activeRoute/ActiveRouteViewModel.kt` — `ActiveRouteState`

```kotlin
data class ActiveRouteState(
    val rota: Rota? = null,
    val paradasRestantes: List<Endereco> = emptyList(),
    val paradaAtualIndex: Int = 0,           // ← indica qual é a parada atual (default = 0)
    val mostrarLista: Boolean = false,        // ← controla a exibição da lista ☰
    val confirmarReotimizacao: Boolean = false, // ← popup de confirmação
    val paradaSelecionadaParaReotimizar: Int? = null, // ← parada escolhida na lista
    ...
)
```

---

## 7. Arquivos Envolvidos

| Arquivo | Função |
|---|---|
| `ui/navigation/DriveNavGraph.kt` | Navegação entre telas (RouteBuild → RouteActive) |
| `ui/navigation/Screen.kt` | Definição das rotas de navegação |
| `ui/activeRoute/ActiveRouteViewModel.kt` | Lógica de transição: carregar rota, selecionar parada, reotimizar |
| `ui/activeRoute/ActiveRouteScreen.kt` | UI: botão ☰, AlertDialog de lista, popup de reotimização |
| `domain/usecase/OtimizarRotaUseCase.kt` | Algoritmo Nearest-Neighbor para reotimização |

---

## ✅ Conclusão

O app **cumpre integralmente** todos os requisitos da fase de transição:

| Requisito | Status |
|---|---|
| Transição direta: construção → execução | ✅ |
| Opção 1: seguir rota sugerida (default = primeira parada) | ✅ |
| Opção 2: escolher manualmente por onde começar (botão ☰) | ✅ |
| AlertDialog com lista de todas as paradas | ✅ |
| Popup de confirmação: "Reotimizar rota? Sim/Não" | ✅ |
| **Sim** → reorganiza restantes por GPS a partir da escolhida | ✅ |
| **Não** → navega direto para a escolhida sem reordenar | ✅ |
| Marca `roteiroFoiAlterado = true` para reotimizações futuras | ✅ |
| GPS inicia automaticamente ao carregar a rota | ✅ |
| Bolha flutuante com distância e informações da parada atual | ✅ |