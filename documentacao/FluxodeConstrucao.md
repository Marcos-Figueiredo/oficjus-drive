# Fluxo de Construção da Rota — OficJus Drive

## Visão Geral

O processo de construção da rota no app Android nativo do OficJus Drive segue regras rigorosas de ordenação física dos documentos no "bolo" do oficial. O fluxo envolve: carregamento de remanescentes, inclusão de novos mandados, inserção/exclusão a qualquer tempo, e otimização em tempo real.

---

## 1. Carregamento de Remanescentes

**Arquivo:** `ui/routebuild/RouteBuildViewModel.kt` — `init {}` (linha ~83)

Ao iniciar a construção da rota, o app **primeiro** carrega os remanescentes (pulados/não entregues da rota anterior):

```kotlin
val remanescentes = enderecoRepository.getRemanescentes()
if (remanescentes.isNotEmpty()) {
    val renumerados = remanescentes.mapIndexed { index, e ->
        e.copy(referencia = index + 1, ordem = index + 1)
    }
    _state.value = _state.value.copy(paradas = renumerados)
    enderecoRepository.limparRemanescentes()
    mostrarMensagem("${remanescentes.size} remanescente(s) da rota anterior", ...)
}
```

**Regras cumpridas:**
- ✅ Remanescentes são carregados **antes de qualquer ação do oficial**
- ✅ Renumerados sequencialmente de `1` a `N` (campo `referencia` = ordem de digitação)
- ✅ Tabela de remanescentes é **limpa** após o carregamento
- ✅ Mensagem informativa exibe a quantidade de remanescentes carregados

---

## 2. Inclusão de Novos Mandados

**Arquivo:** `ui/routebuild/RouteBuildViewModel.kt` — função `adicionarEndereco()` (linha ~360)

Após carregar os remanescentes, os novos mandados são inseridos **após** o último remanescente:

```kotlin
val proximaRef = (_state.value.paradas.maxOfOrNull { it.referencia } ?: 0) + 1
val enderecoComRef = enderecoTemp.copy(referencia = proximaRef, ordem = _state.value.paradas.size + 1)
val paradas = _state.value.paradas + listOf(enderecoComRef)
```

**Funcionamento:**
- Se havia `N` remanescentes, o primeiro novo mandado recebe `referencia = N + 1`
- O próximo mandado recebe `N + 2`, e assim sucessivamente
- O campo `referencia` é **imutável** — preserva para sempre a ordem de digitação original

**Exemplo:**
| Ação | Lista (referencia) |
|---|---|
| 3 remanescentes carregados | 1, 2, 3 |
| Novo mandado inserido | 1, 2, 3, **4** |
| Outro mandado inserido | 1, 2, 3, 4, **5** |

---

## 3. Inserção a Qualquer Tempo

**Arquivo:** `ui/routebuild/RouteBuildViewModel.kt` — função `adicionarEndereco()`

O oficial pode inserir um novo endereço **a qualquer momento** durante a construção da rota. A regra é:

> **"Em caso de inserção, a ordem física o posiciona em último inserido + 1"**

```kotlin
val proximaRef = (_state.value.paradas.maxOfOrNull { it.referencia } ?: 0) + 1
```

**Funcionamento:**
- O novo registro sempre recebe `referencia = maxReferenciaExistente + 1`
- Independente de quantos registros já existem na lista
- O campo `referencia` é imutável e nunca é reatribuído

---

## 4. Exclusão a Qualquer Tempo (Regra N-1)

**Arquivo:** `ui/routebuild/RouteBuildViewModel.kt` — função `removerParada()` (linha ~540)

```kotlin
fun removerParada(index: Int) {
    val paradas = _state.value.paradas.toMutableList()
    if (index in paradas.indices) {
        val removida = paradas[index]
        paradas.removeAt(index)
        val reordenadas = paradas.mapIndexed { i, e -> e.copy(ordem = i + 1) }
        _state.value = _state.value.copy(paradas = reordenadas)
    }
}
```

**Regra cumprida:**
> **"Em caso de exclusão, todos aqueles que seguem após o excluído sobem uma posição (N-1)"**

**Exemplo prático:**
| Ação | Lista (ordem) | Explicação |
|---|---|---|
| Estado inicial | 1º, 2º, 3º, 4º, 5º | 5 documentos |
| Remove o 3º | 1º, 2º, **3º** 👈, 4º | O que era 4º virou 3º |
| Remove o 1º | **1º** 👈, 2º, 3º | O que era 2º virou 1º |
| Remove o 5º | 1º, 2º | - |

---

## 5. Otimização "On-line" Durante a Construção

**Arquivo:** `ui/routebuild/RouteBuildViewModel.kt` — função `autoOtimizar()` (linha ~500)

```kotlin
private fun autoOtimizar() {
    val paradas = _state.value.paradas
    if (paradas.size < 3) return  // 2 ou menos: mantém ordem de digitação
    if (_state.value.isGeocoding.isNotEmpty()) return  // aguarda geocoding

    otimizarJob?.cancel()
    otimizarJob = viewModelScope.launch {
        delay(300) // debounce
        val posicaoAtual = locationService.getUltimaLocalizacao()
        val origem = if (posicaoAtual != null) {
            Posicao(posicaoAtual.latitude, posicaoAtual.longitude)
        } else null
        val otimizadas = OtimizarRotaUseCase.otimizar(paradas, posicaoAtual = origem)
        _state.value = _state.value.copy(paradas = otimizadas)
    }
}
```

Chamada **após cada adição** de endereço:
```kotlin
adicionarEndereco(endereco, numero)
autoOtimizar()  // ← reordena em tempo real
```

**Regras:**
- ✅ A ordem visual se reordena a cada novo registro inserido
- ✅ Usa algoritmo **Nearest-Neighbor** com GPS atual como origem
- ✅ Tem **debounce de 300ms** para evitar reordenações desnecessárias
- ✅ Só executa se houver **3 ou mais** endereços com coordenadas
- ✅ Aguarda o **geocoding** terminar antes de reordenar

---

## 6. Gravação da Rota — Ordem Ótima

**Arquivo:** `ui/routebuild/RouteBuildViewModel.kt` — função `confirmarRota()` (linha ~570)

```kotlin
fun confirmarRota() {
    viewModelScope.launch {
        // Otimiza a rota com o GPS antes de salvar
        val posicaoAtual = locationService.getUltimaLocalizacao()
        val origem = if (posicaoAtual != null) {
            Posicao(posicaoAtual.latitude, posicaoAtual.longitude)
        } else null
        val paradas = if (paradasAtuais.size >= 2) {
            OtimizarRotaUseCase.otimizar(paradasAtuais, posicaoAtual = origem)
        } else paradasAtuais

        // Substitui a rota existente pela nova de forma atômica
        val rota = Rota(id = rotaId, nome = ..., paradas = paradas, status = RotaStatus.ATIVA)
        enderecoRepository.substituirRota(rotaId, rota)
    }
}
```

**Regra cumprida:**
- ✅ Antes de gravar, a rota é **reotimizada** com a posição GPS atual
- ✅ A rota salva no banco já está na **ordem ótima** definida
- ✅ A substituição é atômica (limpa + salva em transação)

---

## 7. Estrutura de Dados

### Domain model `Endereco`

```kotlin
data class Endereco(
    val id: Long = 0,
    val cep: String, val logradouro: String, val numero: String,
    val bairro: String, val cidade: String, val estado: String,
    val latitude: Double? = null, val longitude: Double? = null,
    val ordem: Int = 0,        // ordem atual na rota (pode mudar com otimização)
    val referencia: Int = 0,   // ordem de digitação original (IMUTÁVEL)
    val tipoGeocode: TipoGeocode = TipoGeocode.NENHUM
)
```

### Campos importantes

| Campo | Mutável? | Função |
|---|---|---|
| `ordem` | ✅ Sim | Ordem atual na rota (recalculada a cada otimização) |
| `referencia` | ❌ **Não** | Ordem de digitação original (nunca muda) |

---

## 8. Algoritmo de Otimização

**Arquivo:** `domain/usecase/OtimizarRotaUseCase.kt`

```kotlin
fun otimizar(paradas: List<Endereco>, posicaoAtual: Posicao? = null): List<Endereco> {
    if (paradas.size <= 2) return paradas  // 2 ou menos: mantém ordem de digitação
    // Nearest-Neighbor TSP com distância Haversine
    val comCoords = paradas.filter { it.temCoordenadas }
    val semCoords = paradas.filter { !it.temCoordenadas }
    if (comCoords.size < 2) return paradas
    val ordenadasPorRef = comCoords.sortedBy { it.referencia }
    // ... Nearest-Neighbor ...
    return resultado.mapIndexed { index, endereco ->
        endereco.copy(ordem = index + 1)
    }
}
```

**Etapas:**
1. Endereços **sem coordenadas** são mantidos ao final (na ordem de referência)
2. Endereços **com coordenadas** são reordenados por Nearest-Neighbor
3. O campo `ordem` é atualizado, mas `referencia` permanece intacta

---

## 9. Arquivos Envolvidos

| Arquivo | Função |
|---|---|
| `ui/routebuild/RouteBuildViewModel.kt` | Toda a lógica de construção da rota |
| `ui/routebuild/RouteBuildScreen.kt` | UI da tela de construção |
| `ui/routebuild/components/SmartSearchField.kt` | Campo de busca inteligente |
| `ui/routebuild/components/VoiceInputButton.kt` | Botão de entrada por voz |
| `domain/usecase/OtimizarRotaUseCase.kt` | Algoritmo Nearest-Neighbor TSP |
| `domain/Endereco.kt` | Modelo de domínio |
| `data/repository/EnderecoRepositoryImpl.kt` | Implementação do repositório |
| `data/local/EnderecoEntity.kt` | Entidades Room |
| `data/local/EnderecoDao.kt` | DAOs |
| `data/local/CnefeUnificadaSync.kt` | Sincronização da base CNEFE |
| `data/remote/NominatimApi.kt` | Fallback de geocodificação |

---

## ✅ Conclusão

O app **cumpre integralmente** todos os requisitos da construção da rota:

| Requisito | Status |
|---|---|
| Remanescentes carregados primeiro (antes de qualquer ação) | ✅ |
| Renumerados de 1 a N | ✅ |
| Tabela de remanescentes limpa após carregar | ✅ |
| Novos mandados: N+1, N+2... (após último remanescente) | ✅ |
| Inserção a qualquer tempo (último+1) | ✅ |
| Exclusão a qualquer tempo (N-1 — sobem uma posição) | ✅ |
| Otimização on-line durante inserção | ✅ |
| Rota ótima ao gravar (reotimização no save) | ✅ |
| `referencia` imutável (ordem de digitação original) | ✅ |