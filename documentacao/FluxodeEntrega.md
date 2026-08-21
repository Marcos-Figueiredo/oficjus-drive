# Fluxo de Entrega de Documentos — OficJus Drive

## Visão Geral

O app Android nativo do OficJus Drive implementa um fluxo de entrega de documentos com dois objetivos principais:

1. **Registro de entrega**: ao chegar no endereço, o oficial decide se o documento foi entregue ou não
2. **Controle físico do pacote**: remoção de um documento do "bolo" físico recalcula a posição dos demais (regra p-1)

---

## 1. Bolha Flutuante → Modo Chegada

**Arquivo:** `ui/activeRoute/ActiveRouteViewModel.kt`

Quando o GPS detecta que o usuário está a **< 30m** do destino, a bolha flutuante se transforma automaticamente em modo **"chegada"**:

```kotlin
if (dist < 30 && jaPassouDos30m && dist < ultimaDistanciaChegada
    && BolhaOverlay.isAtiva() && !BolhaOverlay.isModoChegada()) {
    BolhaOverlay.mostrarChegada(
        onSim = { entregueParada(ctx) },
        onNao = { pularParada(ctx) },
        ...
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

## 2. Fluxo "Sim" (Entregue)

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
- Se houver mais paradas: avança para a próxima (reotimiza se roteiro foi alterado)

---

## 3. Fluxo "Não" (Não Entregue)

### 3.1 Via bolha (meio da rota) — `pularParada()`

```kotlin
fun pularParada(context: Context) {
    // Salva como remanescente (banco)
    enderecoRepository.salvarRemanescente(parada)
    // Remove da rota atual (banco)
    enderecoRepository.deletarPorReferencia(rotaId, ref)
    // Remove da lista LOCAL e avança
    ...
}
```

### 3.2 Última parada — `naoEntregueParada()`

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

## 4. Tabela de Remanescentes

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

## 5. Reordenação Física (Regra p-1)

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

O documento 4 que estava na 4ª posição subiu para 3ª (p-1). O documento 2 que estava na 2ª virou 1º. **A regra é cumprida rigorosamente.**

---

## 6. Reotimização Condicional

**Arquivo:** `ui/activeRoute/ActiveRouteViewModel.kt`

```kotlin
private var roteiroFoiAlterado: Boolean = false
```

- `false` (padrão): a rota segue a **ordem planejada** (digitação original) — **não** reordena
- `true`: o usuário alterou a ordem (escolheu via lista ☰) — a partir daí, após entrega/pulo, reotimiza as restantes por GPS

---

## 7. Estrutura de Dados

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

## 8. Arquivos Envolvidos

| Arquivo | Função |
|---|---|
| `ui/activeRoute/ActiveRouteViewModel.kt` | Lógica de entrega, pulo, reordenação, reotimização |
| `ui/activeRoute/ActiveRouteScreen.kt` | UI da tela ativa, popup de chegada, cards de endereço |
| `data/local/EnderecoEntity.kt` | Entities: `EnderecoEntity`, `RotaEntity`, `RemanescenteEntity` |
| `data/local/EnderecoDao.kt` | DAOs: `EnderecoDao`, `RotaDao`, `RemanescenteDao` |
| `data/repository/EnderecoRepositoryImpl.kt` | Implementação do repositório |
| `domain/repository/Repositories.kt` | Interfaces do repositório |
| `domain/Endereco.kt` | Modelo de domínio |
| `ui/routebuild/RouteBuildViewModel.kt` | Carregamento de remanescentes na nova rota |
| `domain/usecase/OtimizarRotaUseCase.kt` | Algoritmo Nearest-Neighbor |
| `data/service/BolhaOverlay.kt` | Serviço da bolha flutuante (overlay) |

---

## ✅ Conclusão

O app **cumpre integralmente** todos os requisitos:

| Requisito | Status |
|---|---|
| Bolha vira modo chegada ao chegar no destino | ✅ |
| Popup "Entregue? Sim / Não" | ✅ |
| **Sim** → deleta da rota (não vira remanescente) | ✅ |
| **Não** → salva em `remanescentes` + deleta da rota | ✅ |
| Tabela de remanescentes | ✅ |
| Remanescentes carregados na próxima rota | ✅ |
| Reordenação p-1 (documentos sobem uma posição) | ✅ |
| Última parada → resumo da rota | ✅ |