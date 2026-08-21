# Fluxo de Entrega de Documentos — OficJus Drive

## Visão Geral

O app Android nativo do OficJus Drive implementa um fluxo de entrega de documentos com três objetivos principais:

1. **Registro de entrega**: ao chegar no endereço, o oficial decide se o documento foi entregue ou não
2. **Controle físico do pacote**: remoção de um documento do "bolo" físico recalcula a posição dos demais (regra p-1)
3. **Reotimização contínua**: a cada parada concluída, a rota é reconstruída pelo GPS para maximizar eficiência

---

## 1. Bolha Flutuante — Modo Navegação

**Arquivo:** `data/service/BolhaOverlay.kt`

Durante a execução da rota, uma bolha flutuante (overlay) permanece sobre o Waze com:

| Elemento | Descrição |
|---|---|
| 🚗 **Distância** | Distância até o destino atual |
| 🚗 **Parada** | Parada atual (ex: "3/15") |
| **☰ Botão esquerdo** | Abre a lista de todas as paradas para selecionar uma específica |
| **⏭️ Botão direito** | Avança/Pula a parada atual (sempre habilitado — inclusive na última) |

```kotlin
// Botão sempre ativo — não depende de podePular
btnDireito?.alpha = 1.0f
btnDireito?.isEnabled = true
btnDireito?.text = "⏭️"
btnDireito?.setOnClickListener { onPular() }
```

### Ações da bolha:

| Botão | Ação | Comportamento |
|---|---|---|
| **☰** | `onLista()` | Abre `AlertDialog` com todas as paradas para seleção |
| **⏭️** | `pularParada()` | Salva como remanescente + deleta da rota + avança |

---

## 2. Bolha Flutuante → Modo Chegada

**Arquivo:** `ui/activeRoute/ActiveRouteViewModel.kt` — `processarLocalizacao()`

Quando o GPS detecta que o usuário está a **< 30m** do destino, a bolha flutuante se transforma automaticamente em modo **"chegada"**:

```kotlin
if (dist < 30 && jaPassouDos30m && dist < ultimaDistanciaChegada
    && BolhaOverlay.isAtiva() && !BolhaOverlay.isModoChegada()) {
    BolhaOverlay.mostrarChegada(
        onSim = { entregueParada(ctx) },
        onNao = { pularParada(ctx) },
        enderecoAtual = textoAtual,
        proximaEntrega = textoProxima
    )
}
```

**Arquivo:** `ui/activeRoute/ActiveRouteScreen.kt`

Um `AlertDialog` é exibido com a pergunta **"Efetuou a entrega?"** e dois botões:

| Botão | Função chamada | Consequência |
|---|---|---|
| **Sim** ✅ | `entregueParada()` | Deleta da rota atual |
| **Não** ❌ | `pularParada()` | Salva como remanescente + deleta da rota |

---

## 3. Fluxo "Sim" (Entregue)

**Arquivo:** `ui/activeRoute/ActiveRouteViewModel.kt` — função `entregueParada()`

```kotlin
fun entregueParada(context: Context) {
    // 1. Remove da lista LOCAL (UI responsiva)
    concluirParadaAtual()  // remove por referencia

    // 2. DELETE no banco (background)
    enderecoRepository.deletarPorReferencia(rotaId, ref)

    // 3. Upsert coordenadas GPS na nuvem (feedback loop CNEFE)
    cnefeRepository.upsertCoordenadas(...)
}
```

**Regras:**
- O registro é **deletado** da tabela `enderecos` (rota atual)
- **Não** vai para a tabela de remanescentes
- Se for a **última parada**: mostra resumo da rota concluída
- Se houver mais paradas: **reotimiza as restantes por GPS** (Nearest-Neighbor)

---

## 4. Fluxo "Não" (Não Entregue)

### 4.1 Via bolha (meio da rota) — `pularParada()`

```kotlin
fun pularParada(context: Context) {
    // Salva como remanescente (banco)
    enderecoRepository.salvarRemanescente(parada)
    // Remove da rota atual (banco)
    enderecoRepository.deletarPorReferencia(rotaId, ref)
    // Remove da lista LOCAL e avança
    ...
    // Se houver mais de 1 restante, reotimiza por GPS
    if (restantes.size > 1) {
        reotimizarParadas(context)
    }
}
```

### 4.2 Última parada — `naoEntregueParada()`

```kotlin
fun naoEntregueParada(context: Context) {
    // Salva como remanescente
    enderecoRepository.salvarRemanescente(parada)
    // Deleta da rota atual
    enderecoRepository.deletarPorReferencia(rotaId, ref)
    // Mostra resumo
}
```

---

## 5. Reotimização por GPS a Cada Parada

**Arquivo:** `ui/activeRoute/ActiveRouteViewModel.kt`

**Comportamento:** a reotimização é **sempre ativa**, não mais condicional ao `roteiroFoiAlterado`.

```kotlin
// Em pularParada():
if (restantes.size > 1) {
    reotimizarParadas(context)  // ← sempre reotimiza
    return
}

// Em entregueParada():
if (state.paradasRestantes.size > 1) {
    reotimizarParadas(context)  // ← sempre reotimiza
}
```

### Função `reotimizarParadas()`

```kotlin
private fun reotimizarParadas(context: Context) {
    val restantes = state.paradasRestantes
    val posicaoGps = state.latitude?.let { lat ->
        state.longitude?.let { lng -> Posicao(lat, lng) }
    }
    val otimizadas = OtimizarRotaUseCase.otimizar(restantes, posicaoGps)
    _state.value = state.copy(paradasRestantes = otimizadas, paradaAtualIndex = 0)
    // Abre Waze para a parada mais próxima
    abrirWaze(context)
    atualizarBolha(context)
}
```

**Fluxo completo:**
1. Usuário entrega ou pula parada
2. Se houver **mais de 1** parada restante → reotimiza por Nearest-Neighbor com GPS atual
3. Se houver **apenas 1** parada restante → avança sem reotimizar
4. Se for a **última** → mostra resumo

---

## 6. Lista de Paradas (Botão ☰)

**Arquivo:** `ui/activeRoute/ActiveRouteScreen.kt` + `ActiveRouteViewModel.kt`

O oficial pode, a qualquer momento, tocar no botão **☰** da bolha para:

1. Abrir `AlertDialog` com todas as paradas restantes
2. Selecionar qualquer endereço como próximo destino
3. Escolher entre:
   - **"Sim, reotimizar"** → reorganiza as restantes por GPS a partir da escolhida
   - **"Não, só navegar"** → navega direto sem reordenar

```kotlin
fun selecionarParada(index: Int) {
    if (index != _state.value.paradaAtualIndex) {
        roteiroFoiAlterado = true
    }
    _state.value = _state.value.copy(paradaAtualIndex = index)
}
```

---

## 7. Waze Reiniciado a Cada Parada

Após cada entrega ou pulo, o Waze é **reaberto** apontando para o próximo endereço:

```kotlin
// Tanto em entregueParada() quanto em pularParada():
if (state.paradaAtual?.temCoordenadas == true) {
    abrirWaze(context)  // ← reabre Waze com coordenadas da próxima parada
}
```

---

## 8. Tabela de Remanescentes

**Arquivo:** `data/local/EnderecoEntity.kt`

```kotlin
@Entity(tableName = "remanescentes")
data class RemanescenteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cep: String, val logradouro: String, val numero: String,
    val bairro: String, val cidade: String, val estado: String,
    val latitude: Double?, val longitude: Double?,
    val ordem: Int = 0, val referencia: Int = 0,
    val salvoEm: Long = System.currentTimeMillis()
)
```

**DAO:** `RemanescenteDao` — operações `inserir`, `getTodos`, `contar`, `limparTodos`

**Repository:** `EnderecoRepositoryImpl` — funções `salvarRemanescente()`, `getRemanescentes()`, `limparRemanescentes()`

### Carregamento na próxima rota

**Arquivo:** `ui/routebuild/RouteBuildViewModel.kt` — `init {}`

```kotlin
val remanescentes = enderecoRepository.getRemanescentes()
if (remanescentes.isNotEmpty()) {
    val renumerados = remanescentes.mapIndexed { index, e ->
        e.copy(referencia = index + 1, ordem = index + 1)
    }
    _state.value = _state.value.copy(paradas = renumerados)
    enderecoRepository.limparRemanescentes()
}
```

---

## 9. Reordenação Física (Regra p-1)

**Arquivo:** `ui/activeRoute/ActiveRouteViewModel.kt` — classe `ActiveRouteState`

```kotlin
fun posicaoNoBolo(referencia: Int): Int {
    val ordenadosPorRef = paradasRestantes.sortedBy { it.referencia }
    val idx = ordenadosPorRef.indexOfFirst { it.referencia == referencia }
    return if (idx >= 0) idx + 1 else 0
}
```

### Exemplo prático da regra p-1

| Ação | Documentos restantes (ref) | Posição no bolo |
|---|---|---|
| Início | 1, 2, 3, 4, 5 | 1º, 2º, 3º, 4º, 5º |
| Entrega doc **3** | 1, 2, 4, 5 | 1º, 2º, **3º** 👈, 4º |
| Entrega doc **1** | 2, 4, 5 | **1º** 👈, 2º, 3º |
| Entrega doc **5** | 2, 4 | 1º, 2º |

---

## 10. Timeout de Inatividade

**Arquivo:** `domain/usecase/SessionTimeoutManager.kt`

| Situação | Timeout |
|---|---|
| **Rota em execução** (ActiveRoute) | ❌ **Suspenso** |
| **Demais telas** (Login, Build, WazeGate) | ⏱️ **5 minutos** |
| **Ao timeout** | Salva remanescentes → deleta rota → fecha Waze → **encerra app** |

---

## 11. Estrutura de Dados

### Domain model `Endereco`

```kotlin
data class Endereco(
    val id: Long = 0,
    val cep: String, val logradouro: String, val numero: String,
    val bairro: String, val cidade: String, val estado: String,
    val latitude: Double? = null, val longitude: Double? = null,
    val ordem: Int = 0,        // ordem atual na rota (pode mudar)
    val referencia: Int = 0,   // ordem de digitação original (imutável)
    val tipoGeocode: TipoGeocode = TipoGeocode.NENHUM
)
```

---

## 12. Arquivos Envolvidos

| Arquivo | Função |
|---|---|
| `ui/activeRoute/ActiveRouteViewModel.kt` | Lógica de entrega, pulo, reordenação, reotimização por GPS |
| `ui/activeRoute/ActiveRouteScreen.kt` | UI da tela ativa, popup de chegada, cards de endereço, lista ☰ |
| `data/service/BolhaOverlay.kt` | Serviço da bolha flutuante (overlay) com botões ☰ e ⏭️ |
| `data/local/EnderecoEntity.kt` | Entities: `EnderecoEntity`, `RotaEntity`, `RemanescenteEntity` |
| `data/local/EnderecoDao.kt` | DAOs: `EnderecoDao`, `RotaDao`, `RemanescenteDao` |
| `data/repository/EnderecoRepositoryImpl.kt` | Implementação do repositório |
| `domain/repository/Repositories.kt` | Interfaces do repositório |
| `domain/Endereco.kt` | Modelo de domínio |
| `domain/usecase/OtimizarRotaUseCase.kt` | Algoritmo Nearest-Neighbor TSP |
| `domain/usecase/SessionTimeoutManager.kt` | Gerenciador de timeout de 5 minutos |
| `ui/routebuild/RouteBuildViewModel.kt` | Carregamento de remanescentes na nova rota |
| `MainActivity.kt` | Detecção de toque para resetar timeout |

---

## ✅ Conclusão

O app **cumpre integralmente** todos os requisitos:

| Requisito | Status |
|---|---|
| Bolha vira modo chegada ao chegar no destino (< 30m) | ✅ |
| Popup "Entregue? Sim / Não" | ✅ |
| **Sim** → deleta da rota (não vira remanescente) | ✅ |
| **Não** → salva em `remanescentes` + deleta da rota | ✅ |
| Botão **⏭️** (pular) sempre habilitado | ✅ |
| Botão **☰** (lista) para selecionar parada específica | ✅ |
| Waze reiniciado a cada parada apontando para o próximo endereço | ✅ |
| **Reotimização por GPS a cada parada** (Nearest-Neighbor) | ✅ |
| Regra p-1 (documentos sobem uma posição quando removidos) | ✅ |
| Tabela de remanescentes | ✅ |
| Remanescentes carregados na próxima rota | ✅ |
| Timeout de 5 minutos (suspenso durante execução) | ✅ |
| Última parada → resumo da rota | ✅ |