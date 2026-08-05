package br.com.oficjus.drive.ui.activeRoute

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.content.Intent
import android.os.Build
import br.com.oficjus.drive.data.service.BolhaOverlay
import br.com.oficjus.drive.data.service.BubbleBolhaService
import br.com.oficjus.drive.data.service.LocationService
import br.com.oficjus.drive.domain.Endereco
import br.com.oficjus.drive.domain.Rota
import br.com.oficjus.drive.domain.repository.EnderecoRepository
import br.com.oficjus.drive.domain.usecase.OtimizarRotaUseCase
import br.com.oficjus.drive.domain.usecase.WazeNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject

data class ActiveRouteState(
    val rota: Rota? = null,
    val paradasRestantes: List<Endereco> = emptyList(),
    val paradasConcluidas: List<Endereco> = emptyList(),
    val paradasPuladas: List<Endereco> = emptyList(),
    val paradaAtualIndex: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val distanciaDestino: String = "---",
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    val paradaAtual: Endereco?
        get() = paradasRestantes.getOrNull(paradaAtualIndex)

    val totalParadas: Int
        get() = paradasConcluidas.size + paradasRestantes.size

    val progresso: String
        get() = "${paradasConcluidas.size}/${totalParadas}"

    fun posicaoNoBolo(referencia: Int): Int {
        val ordenadosPorRef = paradasRestantes.sortedBy { it.referencia }
        val idx = ordenadosPorRef.indexOfFirst { it.referencia == referencia }
        return if (idx >= 0) idx + 1 else 0
    }
}

@HiltViewModel
class ActiveRouteViewModel @Inject constructor(
    private val enderecoRepository: EnderecoRepository,
    private val locationService: LocationService
) : ViewModel() {

    private val _state = MutableStateFlow(ActiveRouteState())
    val state: StateFlow<ActiveRouteState> = _state.asStateFlow()

    override fun onCleared() {
        super.onCleared()
        // Esconde a bolha flutuante quando a tela é destruída
        BolhaOverlay.esconder()
    }

    fun carregarRota(rotaId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val rota = enderecoRepository.getRota(rotaId)
                if (rota != null) {
                    _state.value = _state.value.copy(
                        rota = rota,
                        paradasRestantes = rota.paradas,
                        isLoading = false
                    )
                    iniciarGps()
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
        viewModelScope.launch {
            locationService.locationFlow().collect { location ->
                if (location != null) {
                    val lat = location.latitude
                    val lng = location.longitude
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
                    }
                }
            }
        }
    }

    fun selecionarParada(index: Int) {
        if (index in _state.value.paradasRestantes.indices) {
            _state.value = _state.value.copy(paradaAtualIndex = index)
        }
    }



    /**
     * Conclui a parada atual, remove da lista e reordena as restantes
     * a partir da posição da parada concluída.
     */
    fun concluirParadaAtual() {
        val state = _state.value
        val parada = state.paradaAtual ?: return

        val concluidas = state.paradasConcluidas + parada
        var restantes = state.paradasRestantes.toMutableList()
        restantes.removeAt(state.paradaAtualIndex)

        // Reordena as restantes a partir da posição da parada concluída
        if (restantes.size >= 2 && parada.temCoordenadas) {
            val posicaoAtual = OtimizarRotaUseCase.Posicao(
                latitude = parada.latitude!!,
                longitude = parada.longitude!!
            )
            restantes = OtimizarRotaUseCase.otimizar(
                paradas = restantes,
                posicaoAtual = posicaoAtual
            ).toMutableList()
        }

        _state.value = state.copy(
            paradasRestantes = restantes,
            paradasConcluidas = concluidas,
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
            onEntregue = { entregueParada(context) },
            onLista = { abrirAppComLista(context) }
        )
    }

    fun atualizarBolha(context: Context) {
        val state = _state.value
        val index = state.paradaAtualIndex
        val total = state.paradasRestantes.size
        val podePular = index < total - 1
        BolhaOverlay.atualizar(
            state.distanciaDestino,
            "${index + 1}/$total",
            podePular
        )
    }

    fun esconderBolha(context: Context) {
        BolhaOverlay.esconder()
    }

    /**
     * Pular: avança para a próxima parada na ordem atual.
     * O endereço permanece na lista (não é removido).
     */
    fun pularParada(context: Context) {
        val state = _state.value
        val next = state.paradaAtualIndex + 1
        if (next < state.paradasRestantes.size) {
            _state.value = state.copy(paradaAtualIndex = next)
            reordenarPorGps()
            abrirWaze(context)
            atualizarBolha(context)
        }
    }

    /**
     * Entregue: deleta do banco (síncrono), marca como concluída e avança.
     */
    fun entregueParada(context: Context) {
        val parada = _state.value.paradaAtual ?: return
        val rotaId = _state.value.rota?.id ?: return
        val ref = parada.referencia
        // Deleta do banco antes de concluir (garante que o registro sumiu)
        viewModelScope.launch {
            enderecoRepository.deletarPorReferencia(rotaId, ref)
            // Após deletar, conclui e avança
            concluirParadaAtual()
            val state = _state.value
            if (state.paradaAtual != null) {
                abrirWaze(context)
                atualizarBolha(context)
            } else {
                esconderBolha(context)
            }
        }
    }

    /**
     * Abre o app na tela de rota ativa para o usuário escolher um endereço.
     */
    fun abrirAppComLista(context: Context) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        if (launchIntent != null) {
            launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            context.startActivity(launchIntent)
        }
    }

    /**
     * Seleciona uma parada da lista, atualiza a bolha e abre o Waze.
     */
    fun selecionarParadaDaLista(index: Int, context: Context) {
        if (index in _state.value.paradasRestantes.indices) {
            _state.value = _state.value.copy(paradaAtualIndex = index)
            abrirWaze(context)
            atualizarBolha(context)
        }
    }

    /**
     * Reordena as paradas restantes pela posição atual do GPS.
     * Chamado após cada entrega ou pulo.
     */
    private fun reordenarPorGps() {
        val state = _state.value
        if (state.paradasRestantes.size < 2) return
        val lat = state.latitude ?: return
        val lng = state.longitude ?: return
        val posicao = OtimizarRotaUseCase.Posicao(latitude = lat, longitude = lng)
        val reordenadas = OtimizarRotaUseCase.otimizar(
            paradas = state.paradasRestantes,
            posicaoAtual = posicao
        )
        _state.value = state.copy(
            paradasRestantes = reordenadas,
            paradaAtualIndex = 0
        )
    }

    /**
     * Finaliza a rota: NÃO limpa o DB (rota eterna).
     * Os pulados permanecem no banco para a próxima sessão.
     */
    fun finalizarRota(context: Context) {
        esconderBolha(context)
        val state = _state.value
        _state.value = state.copy(
            rota = null,
            paradasRestantes = emptyList(),
            paradasConcluidas = emptyList()
        )
    }

    val isRotaConcluida: Boolean
        get() = _state.value.paradasRestantes.isEmpty() && _state.value.paradasConcluidas.isNotEmpty()
}
