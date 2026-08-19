package br.com.oficjus.drive.ui.activeRoute

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.content.Intent
import br.com.oficjus.drive.data.service.BolhaOverlay
import br.com.oficjus.drive.data.service.LocationService
import br.com.oficjus.drive.data.service.RouteTrackingService
import br.com.oficjus.drive.data.repository.SyncPendenteRepository
import br.com.oficjus.drive.domain.Endereco
import br.com.oficjus.drive.domain.Rota
import br.com.oficjus.drive.domain.repository.CnefeRepository
import br.com.oficjus.drive.domain.repository.EnderecoRepository
import br.com.oficjus.drive.domain.usecase.OtimizarRotaUseCase
import br.com.oficjus.drive.domain.usecase.WazeNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ActiveRouteState(
    val rota: Rota? = null,
    val paradasRestantes: List<Endereco> = emptyList(),
    val paradaAtualIndex: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val distanciaDestino: String = "---",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val mensagem: String? = null,
    val mensagemTipo: MensagemTipo = MensagemTipo.INFO,
    val mostrarLista: Boolean = false,
    val mostrarChegada: Boolean = false,
    val totalInicial: Int = 0,
    val entregues: Int = 0,
    val naoEntregues: Int = 0,
    val confirmarReotimizacao: Boolean = false,
    val paradaSelecionadaParaReotimizar: Int? = null
) {
    val paradaAtual: Endereco?
        get() = paradasRestantes.getOrNull(paradaAtualIndex)

    val progresso: String
        get() = "${paradasRestantes.size} parada(s) restante(s)"

    fun posicaoNoBolo(referencia: Int): Int {
        val ordenadosPorRef = paradasRestantes.sortedBy { it.referencia }
        val idx = ordenadosPorRef.indexOfFirst { it.referencia == referencia }
        return if (idx >= 0) idx + 1 else 0
    }
}

enum class MensagemTipo { INFO, SUCCESS, ERROR }

@HiltViewModel
class ActiveRouteViewModel @Inject constructor(
    private val enderecoRepository: EnderecoRepository,
    private val locationService: LocationService,
    private val cnefeRepository: CnefeRepository,
    private val syncPendenteRepository: SyncPendenteRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ActiveRouteState())
    val state: StateFlow<ActiveRouteState> = _state.asStateFlow()
    private var appContext: Context? = null
    private var ultimaDistanciaChegada: Double = Double.MAX_VALUE
    private var jaPassouDos30m: Boolean = false
    private var onVoltarCallback: (() -> Unit)? = null

    /**
     * Controle de reotimização condicional:
     * - false: a rota segue a ordem planejada (digitação) — NÃO reordena
     * - true: o usuário alterou a ordem (escolheu via lista ☰) —
     *         a partir daí, após entrega/pulo, reotimiza as restantes por GPS
     *         (excluindo entregues e pulados do processo de reordenação)
     */
    private var roteiroFoiAlterado: Boolean = false

    fun mostrarMensagem(texto: String, tipo: MensagemTipo = MensagemTipo.INFO) {
        _state.value = _state.value.copy(mensagem = texto, mensagemTipo = tipo)
        viewModelScope.launch {
            delay(3000)
            _state.value = _state.value.copy(mensagem = null)
        }
    }

    override fun onCleared() {
        super.onCleared()
        appContext?.let { RouteTrackingService.stop(it) }
        BolhaOverlay.limparEstado()
        BolhaOverlay.esconder()
    }

    fun carregarRota(rotaId: String, context: Context? = null, onVoltar: (() -> Unit)? = null) {
        // Guarda o callback de navegação para usar em pularParada (última parada)
        if (onVoltar != null) onVoltarCallback = onVoltar
        // Seta o contexto o mais cedo possível para a bolha de chegada
        if (context != null) {
            appContext = context.applicationContext
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val rota = enderecoRepository.getRota(rotaId)
                if (rota != null) {
                    _state.value = _state.value.copy(
                        rota = rota,
                        paradasRestantes = rota.paradas,
                        totalInicial = rota.paradas.size,
                        entregues = 0,
                        naoEntregues = 0,
                        isLoading = false
                    )
                    iniciarGps()
                    // Tenta reenviar coordenadas pendentes (offline queue)
                    processarFilaPendente()
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Rota não encontrada"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Erro ao carregar rota: ${e.message}"
                )
            }
        }
    }

    private fun iniciarGps() {
        // Inicia o Foreground Service para manter o GPS ativo em background
        appContext?.let { RouteTrackingService.start(it) }

        viewModelScope.launch {
            // Coleciona do RouteTrackingService (foreground) — funciona em background
            RouteTrackingService.ultimaLocalizacao.collect { location ->
                if (location != null) {
                    processarLocalizacao(location.latitude, location.longitude)
                }
            }
        }

        // Fallback: também coleta do LocationService (se o serviço não estiver ativo)
        viewModelScope.launch {
            locationService.locationFlow().collect { location ->
                if (location != null) {
                    processarLocalizacao(location.latitude, location.longitude)
                }
            }
        }
    }

    private fun processarLocalizacao(lat: Double, lng: Double) {
        _state.value = _state.value.copy(
            latitude = lat,
            longitude = lng
        )

        // Calcula distância até a parada atual
        val parada = _state.value.paradaAtual
        if (parada?.temCoordenadas == true) {
            val dist = OtimizarRotaUseCase.haversine(
                lat to lng,
                parada.latitude!! to parada.longitude!!
            )
            val texto = if (dist >= 1000) {
                "${"%.1f".format(dist / 1000)}km"
            } else {
                "${dist.toInt()}m"
            }
            _state.value = _state.value.copy(
                distanciaDestino = "🚗 $texto"
            )

            // Atualiza a distância na bolha flutuante em tempo real
            BolhaOverlay.atualizar(
                distancia = "🚗 $texto",
                parada = "${_state.value.paradaAtualIndex + 1}/${_state.value.paradasRestantes.size}",
                podePular = _state.value.paradaAtualIndex < _state.value.paradasRestantes.size - 1
            )
            BolhaOverlay.salvarUltimosValores(
                distancia = "🚗 $texto",
                parada = "${_state.value.paradaAtualIndex + 1}/${_state.value.paradasRestantes.size}",
                podePular = _state.value.paradaAtualIndex < _state.value.paradasRestantes.size - 1
            )

            // Se a distância aumentou muito (>100m), o usuário passou do destino —
            // reseta para que a bolha possa reaparecer quando ele voltar
            if (dist > ultimaDistanciaChegada + 100) {
                ultimaDistanciaChegada = Double.MAX_VALUE
            }

            // Marca que o usuário já passou dos 30m (evita ativação na hora)
            if (dist > 30) jaPassouDos30m = true

            // Detecta chegada (< 30m) — transforma a bolha em modo "chegada"
            // Só ativa se o usuário já passou dos 30m antes (evita falso no carregamento)
            android.util.Log.w("DRIVE_CHEGADA", "dist=${dist.toInt()}m jaPassou=$jaPassouDos30m ultimaCheg=${ultimaDistanciaChegada.toInt()} isAtiva=${BolhaOverlay.isAtiva()} isCheg=${BolhaOverlay.isModoChegada()} temCoords=${parada?.temCoordenadas}")
            if (dist < 30 && jaPassouDos30m && dist < ultimaDistanciaChegada && BolhaOverlay.isAtiva() && !BolhaOverlay.isModoChegada()) {
                ultimaDistanciaChegada = dist
                val ctx = appContext ?: return

                // Dados para os cards da bolha de chegada
                val stateChegada = _state.value
                val atual = stateChegada.paradaAtual
                val proxima = stateChegada.paradasRestantes.getOrNull(stateChegada.paradaAtualIndex + 1)

                // Card 1: endereço atual (resumo)
                val textoAtual = atual?.let {
                    "${it.logradouro}, Nº ${it.numero} - ${it.bairro}"
                } ?: ""

                // Card 2: próxima entrega (completo)
                val textoProxima = if (proxima != null) {
                    "Próxima: ${proxima.logradouro}, Nº ${proxima.numero} - ${proxima.bairro}"
                } else {
                    ""
                }

                BolhaOverlay.mostrarChegada(
                    onSim = {
                        BolhaOverlay.restaurarModoNavegacao()
                        entregueParada(ctx)
                    },
                    onNao = {
                        BolhaOverlay.restaurarModoNavegacao()
                        pularParada(ctx)
                    },
                    enderecoAtual = textoAtual,
                    proximaEntrega = textoProxima
                )
            }
        }
    }

    fun selecionarParada(index: Int) {
        if (index in _state.value.paradasRestantes.indices) {
            // Seleção manual de parada = roteiro alterado pelo usuário
            if (index != _state.value.paradaAtualIndex) {
                roteiroFoiAlterado = true
            }
            _state.value = _state.value.copy(paradaAtualIndex = index)
        }
    }



    /**
     * Conclui a parada atual: remove da lista pela referencia (imutável) e reordena.
     * Remove por referencia em vez de índice para evitar erro se a lista foi reordenada.
     */
    fun concluirParadaAtual() {
        val state = _state.value
        val parada = state.paradaAtual ?: return
        val ref = parada.referencia

        var restantes = state.paradasRestantes.toMutableList()
        restantes.removeAll { it.referencia == ref }

        _state.value = state.copy(
            paradasRestantes = restantes,
            paradaAtualIndex = 0
        )
    }

    fun abrirWaze(context: Context) {
        val parada = _state.value.paradaAtual ?: return
        WazeNavigator.abrirParaEndereco(context, parada)
    }

    /**
     * Abre o Waze e mostra a bolha.
     */
    fun abrirWazeComBolha(context: Context) {
        iniciarBolha(context)
        abrirWaze(context)
    }

    private fun iniciarBolha(context: Context) {
        appContext = context.applicationContext
        val state = _state.value
        if (state.paradaAtual == null) return
        val index = state.paradaAtualIndex
        val total = state.paradasRestantes.size

        val podePular = index < total - 1

        BolhaOverlay.mostrar(
            context = context,
            distancia = state.distanciaDestino,
            parada = "${index + 1}/$total",
            podePular = podePular,
            onPular = { pularParada(context) },
            onLista = { abrirAppComLista(context) }
        )
    }

    fun atualizarBolha(context: Context) {
        val state = _state.value
        val index = state.paradaAtualIndex
        val total = state.paradasRestantes.size
        val podePular = index < total - 1
        // Se a bolha foi removida (restaurarModoNavegacao), recria
        if (!BolhaOverlay.isAtiva()) {
            iniciarBolha(context)
        } else {
            BolhaOverlay.atualizar(
                state.distanciaDestino,
                "${index + 1}/$total",
                podePular
            )
        }
    }

    fun esconderBolha(context: Context) {
        BolhaOverlay.esconder()
    }

    /**
     * Pular/NÃO: salva a parada como remanescente (próxima rota) e remove da rota atual.
     * Se for a última, mostra o resumo.
     */
    fun pularParada(context: Context) {
        val state = _state.value
        val parada = state.paradaAtual ?: return
        val rotaId = state.rota?.id
        val ref = parada.referencia

        // Salva como remanescente (banco) — volta na próxima rota
        viewModelScope.launch(Dispatchers.IO) {
            try { enderecoRepository.salvarRemanescente(parada) } catch (_: Exception) {}
            // Remove da rota atual (banco)
            if (rotaId != null) {
                try { enderecoRepository.deletarPorReferencia(rotaId, ref) } catch (_: Exception) {}
            }
        }

        // Remove da lista LOCAL e avança
        val restantes = state.paradasRestantes.toMutableList()
        restantes.removeAll { it.referencia == ref }
        val next = state.paradaAtualIndex.coerceAtMost(restantes.size - 1)

        _state.value = state.copy(
            paradasRestantes = restantes,
            paradaAtualIndex = next,
            naoEntregues = state.naoEntregues + 1
        )
        ultimaDistanciaChegada = Double.MAX_VALUE

        // Última parada: mostra o resumo
        if (restantes.isEmpty()) {
            trazerAppParaFrente(context)
            return
        }

        // Se o usuário alterou o roteiro, reotimiza as restantes por GPS
        if (roteiroFoiAlterado && restantes.size > 1) {
            reotimizarParadas(context)
            return
        }

        // Avança para a próxima parada
        if (_state.value.paradaAtual?.temCoordenadas == true) {
            abrirWaze(context)
        }
        atualizarBolha(context)
    }

    /**
     * Reotimiza as paradas restantes por Nearest-Neighbor (GPS atual como origem),
     * ignorando entregues (já removidos da lista) e pulados (permanecem,
     * mas entram na nova ordem pela proximidade).
     * Usado somente quando o usuário alterou o roteiro (escolheu fora da ordem).
     */
    private fun reotimizarParadas(context: Context) {
        val state = _state.value
        val restantes = state.paradasRestantes
        if (restantes.isEmpty()) return

        val posicaoGps = state.latitude?.let { lat ->
            state.longitude?.let { lng -> OtimizarRotaUseCase.Posicao(lat, lng) }
        }

        val otimizadas = OtimizarRotaUseCase.otimizar(restantes, posicaoGps)
        _state.value = state.copy(
            paradasRestantes = otimizadas,
            paradaAtualIndex = 0,
            naoEntregues = state.naoEntregues + 1
        )
        ultimaDistanciaChegada = Double.MAX_VALUE

        // Navega para a parada mais próxima
        val proxima = _state.value.paradaAtual
        if (proxima?.temCoordenadas == true) {
            abrirWaze(context)
        }
        atualizarBolha(context)
    }

    /**
     * Entregue: deleta do banco, remove da lista local, upsert coordenadas na nuvem e avança.
     */
    fun entregueParada(context: Context) {
        val parada = _state.value.paradaAtual ?: return
        val rotaId = _state.value.rota?.id ?: return
        val ref = parada.referencia

        // Remove do estado LOCAL imediatamente (UI responsiva)
        _state.value = _state.value.copy(entregues = _state.value.entregues + 1)
        concluirParadaAtual()
        ultimaDistanciaChegada = Double.MAX_VALUE

        val state = _state.value
        val temProxima = state.paradaAtual != null
        val latitude = state.latitude
        val longitude = state.longitude

        // DELETE em background + upsert coordenadas na nuvem
        viewModelScope.launch(Dispatchers.IO) {
            try {
                enderecoRepository.deletarPorReferencia(rotaId, ref)
            } catch (_: Exception) { }

            // Upsert coordenadas colhidas pelo GPS para retroalimentar a base
            if (latitude != null && longitude != null && parada.cep.isNotBlank() && parada.numero.isNotBlank()) {
                try {
                    cnefeRepository.upsertCoordenadas(
                        cep = parada.cep,
                        numero = parada.numero,
                        latitude = latitude,
                        longitude = longitude,
                        cidade = parada.cidade,
                        logradouroId = 0
                    )
                } catch (_: Exception) {
                    // Se falhou (sem internet), salva na fila offline para retry depois
                    try {
                        syncPendenteRepository.adicionar(
                            cep = parada.cep,
                            numero = parada.numero,
                            latitude = latitude,
                            longitude = longitude,
                            cidade = parada.cidade,
                            logradouroId = 0
                        )
                    } catch (_: Exception) { }
                }
            }
        }

        if (temProxima) {
            // Se o usuário alterou o roteiro, reotimiza as restantes por GPS
            if (roteiroFoiAlterado) {
                reotimizarParadas(context)
            } else {
                // Avança para a próxima parada (ordem planejada)
                if (state.paradaAtual?.temCoordenadas == true) {
                    abrirWaze(context)
                }
                atualizarBolha(context)
            }
        } else {
            // ÚLTIMA parada entregue: mostra o resumo da rota concluída
            // Finaliza o Waze, esconde a bolha e traz o app para frente
            RouteTrackingService.stop(context)
            WazeNavigator.matarWaze(context)
            esconderBolha(context)
            BolhaOverlay.limparEstado()
            trazerAppParaFrente(context)
        }
    }

    /**
     * NÃO entregue (última parada): salva como remanescente, remove da rota, mostra resumo.
     */
    fun naoEntregueParada(context: Context) {
        val state = _state.value
        val parada = state.paradaAtual
        val rotaId = state.rota?.id

        // Salva como remanescente (banco) — volta na próxima rota
        if (parada != null) {
            viewModelScope.launch(Dispatchers.IO) {
                try { enderecoRepository.salvarRemanescente(parada) } catch (_: Exception) {}
                if (rotaId != null) {
                    try { enderecoRepository.deletarPorReferencia(rotaId, parada.referencia) } catch (_: Exception) {}
                }
            }
        }

        // Remove da lista LOCAL
        _state.value = state.copy(
            paradasRestantes = emptyList(),
            paradaAtualIndex = 0,
            naoEntregues = state.naoEntregues + 1
        )
        ultimaDistanciaChegada = Double.MAX_VALUE

        // Desliga Waze, GPS, bolha e traz app para frente (mostra resumo)
        RouteTrackingService.stop(context)
        WazeNavigator.matarWaze(context)
        esconderBolha(context)
        BolhaOverlay.limparEstado()
        trazerAppParaFrente(context)
    }

    /**
     * Abre o app na tela de rota ativa para o usuário escolher um endereço.
     * Traz o app para o foreground (volta do Waze) e mostra a lista de paradas.
     */
    fun abrirAppComLista(context: Context) {
        // Seta a flag primeiro (antes de trazer o app, pra tela pegar o estado)
        _state.value = _state.value.copy(mostrarLista = true)
        // Traz o app para o foreground (volta do Waze)
        trazerAppParaFrente(context)
    }

    fun fecharLista() {
        _state.value = _state.value.copy(mostrarLista = false)
    }

    fun fecharChegada() {
        _state.value = _state.value.copy(mostrarChegada = false)
    }

    private fun processarFilaPendente() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                syncPendenteRepository.processarFila()
            } catch (_: Exception) { }
        }
    }

    /**
     * Seleciona uma parada da lista, atualiza a bolha e abre o Waze.
     *
     * Comportamento inteligente de reotimização:
     * - Antes de iniciar navegação (bolha não ativa): reotimiza automaticamente
     *   — a parada selecionada vira a primeira, as demais são ordenadas
     *     por proximidade a partir da coordenada de CHEGADA da parada selecionada
     *     (entregues e pulados são excluídos do processo de reordenação)
     * - Durante a navegação (bolha ativa): pergunta ao usuário se deseja reotimizar
     */
    fun selecionarParadaDaLista(index: Int, context: Context) {
        if (index !in _state.value.paradasRestantes.indices) return

        // Se a navegação já começou (bolha ativa), pergunta primeiro
        if (BolhaOverlay.isAtiva()) {
            _state.value = _state.value.copy(
                confirmarReotimizacao = true,
                paradaSelecionadaParaReotimizar = index
            )
            return
        }

        // Antes da navegação: reotimiza automaticamente
        reotimizarAPartirDe(index, context)
    }

    /** Confirma a reotimização (usuário respondeu "sim" no popup). */
    fun confirmarReotimizacao(context: Context) {
        val index = _state.value.paradaSelecionadaParaReotimizar ?: return
        _state.value = _state.value.copy(
            confirmarReotimizacao = false,
            paradaSelecionadaParaReotimizar = null
        )
        reotimizarAPartirDe(index, context)
    }

    /** Cancela a reotimização (usuário respondeu "não" — só navega para a parada). */
    fun cancelarReotimizacao(context: Context) {
        val index = _state.value.paradaSelecionadaParaReotimizar ?: return
        _state.value = _state.value.copy(
            confirmarReotimizacao = false,
            paradaSelecionadaParaReotimizar = null
        )
        // Só navega para a parada sem reotimizar
        _state.value = _state.value.copy(paradaAtualIndex = index)
        abrirWaze(context)
        atualizarBolha(context)
    }

    /**
     * Reotimiza a rota a partir da parada selecionada:
     * 1. Coloca a parada selecionada como primeira
     * 2. Ordena as restantes por Nearest-Neighbor a partir da coordenada
     *    de CHEGADA da parada selecionada
     * 3. Entregues já foram removidos, pulados permanecem mas entram na nova ordem
     */
    private fun reotimizarAPartirDe(index: Int, context: Context) {
        val state = _state.value
        val restantes = state.paradasRestantes.toMutableList()
        if (restantes.isEmpty()) return

        // A parada escolhida sai da lista e vira a primeira
        val escolhida = restantes.removeAt(index)

        // Ponto de partida da reotimização: coordenada da parada escolhida (CHEGADA)
        // Se não tiver coordenadas, usa o GPS atual
        val pontoPartida = if (escolhida.temCoordenadas) {
            OtimizarRotaUseCase.Posicao(escolhida.latitude!!, escolhida.longitude!!)
        } else {
            state.latitude?.let { lat ->
                state.longitude?.let { lng -> OtimizarRotaUseCase.Posicao(lat, lng) }
            }
        }

        // Otimiza as restantes por Nearest-Neighbor a partir do ponto de partida
        val otimizadas = if (pontoPartida != null) {
            OtimizarRotaUseCase.otimizar(restantes, pontoPartida)
        } else {
            restantes
        }

        // Monta a nova lista: escolhida primeiro, depois as otimizadas
        val novaOrdem = listOf(escolhida) + otimizadas

        _state.value = state.copy(
            paradasRestantes = novaOrdem,
            paradaAtualIndex = 0,
            confirmarReotimizacao = false,
            paradaSelecionadaParaReotimizar = null
        )
        roteiroFoiAlterado = true
        ultimaDistanciaChegada = Double.MAX_VALUE

        // Navega para a primeira parada (a escolhida)
        if (escolhida.temCoordenadas) {
            abrirWaze(context)
        }
        atualizarBolha(context)
    }

    fun fecharReotimizacao() {
        _state.value = _state.value.copy(
            confirmarReotimizacao = false,
            paradaSelecionadaParaReotimizar = null
        )
    }

    /**
     * Finaliza a rota: só limpa o estado local.
     * Limpa o estado local e a rota do banco (paradas já foram salvas em remanescentes).
     */
    fun finalizarRota(context: Context, onVoltar: (() -> Unit)? = null) {
        // Para o Foreground Service de GPS
        RouteTrackingService.stop(context)

        BolhaOverlay.limparEstado()
        esconderBolha(context)
        WazeNavigator.matarWaze(context)

        // Usa o callback fornecido ou o armazenado (setado em carregarRota)
        val callback = onVoltar ?: onVoltarCallback

        // Limpa o estado LOCAL imediatamente
        val rotaId = _state.value.rota?.id
        _state.value = _state.value.copy(
            rota = null,
            paradasRestantes = emptyList()
        )

        // Limpa a rota do banco (remanescentes já foram salvas separadamente)
        if (rotaId != null) {
            viewModelScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        enderecoRepository.limparRota(rotaId)
                    }
                } catch (_: Exception) { }
                callback?.invoke()
            }
        } else {
            callback?.invoke()
        }
    }

    /**
     * Traz o app para o primeiro plano, empurrando o Waze para segundo plano.
     * O Android gerencia o Waze naturalmente depois disso.
     */
    private fun trazerAppParaFrente(context: Context) {
        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                context.startActivity(launchIntent)
            }
        } catch (_: Exception) { }
    }
}
