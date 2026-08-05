package br.com.oficjus.drive.ui.routebuild

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.oficjus.drive.domain.Endereco
import br.com.oficjus.drive.domain.Rota
import br.com.oficjus.drive.domain.RotaStatus
import br.com.oficjus.drive.domain.repository.CnefeRepository
import br.com.oficjus.drive.domain.repository.EnderecoRepository
import br.com.oficjus.drive.domain.repository.AuthRepository
import br.com.oficjus.drive.domain.usecase.OtimizarRotaUseCase
import br.com.oficjus.drive.data.remote.NominatimApi
import br.com.oficjus.drive.data.service.LocationService
import br.com.oficjus.drive.domain.usecase.OtimizarRotaUseCase.Posicao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class RouteBuildState(
    val searchText: String = "",
    val sugestoes: List<Endereco> = emptyList(),
    val paradas: List<Endereco> = emptyList(),
    val isLoading: Boolean = false,
    val isOptimizing: Boolean = false,
    val isSaving: Boolean = false,
    val isGeocoding: Set<Int> = emptySet(),
    val confirmandoEndereco: Endereco? = null,
    val numeroDigitado: String = "",
    val mensagem: String? = null,
    val mensagemTipo: MensagemTipo = MensagemTipo.INFO,
    val rotaConfirmadaId: String? = null,
    val enderecoDuplicado: EnderecoDuplicado? = null
)

data class EnderecoDuplicado(
    val cep: String,
    val numero: String,
    val logradouro: String,
    val referenciaExistente: Int,
    val posicaoNoBolo: Int
)

enum class MensagemTipo { INFO, SUCCESS, ERROR }

@HiltViewModel
class RouteBuildViewModel @Inject constructor(
    private val cnefeRepository: CnefeRepository,
    private val enderecoRepository: EnderecoRepository,
    private val nominatimApi: NominatimApi,
    private val locationService: LocationService,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RouteBuildState())
    val state: StateFlow<RouteBuildState> = _state.asStateFlow()

    private var searchJob: Job? = null

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    init {
        viewModelScope.launch {
            // Rota eterna: carrega remanescentes da rota ativa primeiro
            val rotaAtiva = enderecoRepository.getRotaAtiva()
            if (rotaAtiva != null && rotaAtiva.paradas.isNotEmpty()) {
                _state.value = _state.value.copy(
                    paradas = rotaAtiva.paradas,
                    mensagem = "${rotaAtiva.paradas.size} remanescente(s) da rota anterior",
                    mensagemTipo = MensagemTipo.INFO
                )
            } else {
                val avulsos = enderecoRepository.getEnderecosAvulsos()
                if (avulsos.isNotEmpty()) {
                    _state.value = _state.value.copy(paradas = avulsos)
                }
            }
        }
    }

    fun onSearchTextChanged(text: String) {
        _state.value = _state.value.copy(
            searchText = text,
            sugestoes = emptyList(),
            mensagem = null  // limpa mensagem anterior
        )

        // Limpa formatação (hífen, ponto, espaço) para detectar CEP
        val apenasDigitos = text.filter { it.isDigit() }
        if (apenasDigitos.length == 8) {
            buscarCep(apenasDigitos)
            return
        }

        if (text.any { it.isLetter() } && text.length >= 3) {
            searchJob?.cancel()
            searchJob = viewModelScope.launch {
                delay(400)
                buscarLogradouro(text)
            }
        } else {
            _state.value = _state.value.copy(sugestoes = emptyList())
        }
    }

    private suspend fun buscarLogradouro(termo: String) {
        _state.value = _state.value.copy(isLoading = true)
        try {
            val resultados = cnefeRepository.buscarPorLogradouro(termo)

            // Ordena por distância do GPS, se disponível
            val posicaoGps = locationService.getUltimaLocalizacao()
            val ordenados = if (posicaoGps != null) {
                val origem = Posicao(posicaoGps.latitude, posicaoGps.longitude)
                resultados.sortedBy { endereco ->
                    if (endereco.temCoordenadas) {
                        OtimizarRotaUseCase.haversine(
                            origem.latitude to origem.longitude,
                            endereco.latitude!! to endereco.longitude!!
                        )
                    } else Double.MAX_VALUE
                }
            } else resultados

            _state.value = _state.value.copy(
                sugestoes = ordenados,
                isLoading = false
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                sugestoes = emptyList(),
                isLoading = false,
                mensagem = "Erro ao buscar: ${e.message}",
                mensagemTipo = MensagemTipo.ERROR
            )
        }
    }

    private fun buscarCep(cep: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val resultadosCNEFE = cnefeRepository.buscarPorCep(cep)
                if (resultadosCNEFE.isNotEmpty()) {
                    val dto = resultadosCNEFE.first()
                    _state.value = _state.value.copy(
                        isLoading = false,
                        searchText = "",
                        confirmandoEndereco = dto,
                        numeroDigitado = ""
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        mensagem = "CEP não encontrado no CNEFE",
                        mensagemTipo = MensagemTipo.ERROR
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    mensagem = "Erro: ${e.message}",
                    mensagemTipo = MensagemTipo.ERROR
                )
            }
        }
    }

    fun onSelecionarSugestao(endereco: Endereco) {
        _state.value = _state.value.copy(
            searchText = "",
            sugestoes = emptyList(),
            confirmandoEndereco = endereco,
            numeroDigitado = ""
        )
    }

    fun onNumeroConfirmacaoChanged(numero: String) {
        _state.value = _state.value.copy(numeroDigitado = numero.filter { it.isDigit() })
    }

    fun confirmarNumero() {
        val endereco = _state.value.confirmandoEndereco ?: return
        val numero = _state.value.numeroDigitado

        if (numero.isBlank()) {
            _state.value = _state.value.copy(
                mensagem = "Digite o número do imóvel",
                mensagemTipo = MensagemTipo.ERROR
            )
            return
        }

        // Verifica se já existe endereço com mesmo CEP + número
        val duplicado = _state.value.paradas.firstOrNull {
            it.cep == endereco.cep && it.numero == numero
        }
        if (duplicado != null) {
            val posicao = _state.value.paradas
                .sortedBy { it.referencia }
                .indexOfFirst { it.referencia == duplicado.referencia } + 1
            _state.value = _state.value.copy(
                enderecoDuplicado = EnderecoDuplicado(
                    cep = endereco.cep,
                    numero = numero,
                    logradouro = endereco.logradouro,
                    referenciaExistente = duplicado.referencia,
                    posicaoNoBolo = posicao
                )
            )
            return
        }

        adicionarEndereco(endereco, numero)
    }

    fun ignorarDuplicata() {
        val dup = _state.value.enderecoDuplicado ?: return
        _state.value = _state.value.copy(enderecoDuplicado = null)
    }

    fun cancelarDuplicata() {
        _state.value = _state.value.copy(enderecoDuplicado = null)
    }

    private fun adicionarEndereco(endereco: Endereco, numero: String) {

        // Zera coordenadas — serão buscadas em background com CEP+número exato
        val enderecoTemp = endereco.copy(numero = numero, latitude = null, longitude = null)
        val proximaRef = (_state.value.paradas.maxOfOrNull { it.referencia } ?: 0) + 1
        val enderecoComRef = enderecoTemp.copy(referencia = proximaRef)

        val paradas = listOf(enderecoComRef.copy(ordem = 1)) + _state.value.paradas.map { it.copy(ordem = it.ordem + 1) }
        _state.value = _state.value.copy(
            paradas = paradas,
            confirmandoEndereco = null,
            numeroDigitado = "",
            isGeocoding = _state.value.isGeocoding + proximaRef
        )

        // Busca coordenadas: CNEFE exato → ±10 → interpolação → Nominatim
        viewModelScope.launch {
            var encontrouCoord = false
            try {
                val numeroInt = numero.toIntOrNull()

                // 1. Tenta CNEFE com número exato ou ±10
                val resultados = cnefeRepository.buscarPorCepENumero(endereco.cep, numero)
                var comCoord = resultados.firstOrNull { it.temCoordenadas }
                if (comCoord == null && numeroInt != null) {
                    // Tenta ±10 do número
                    for (delta in 1..10) {
                        if (comCoord != null) break
                        val candidatos = cnefeRepository.buscarPorCepENumero(endereco.cep, (numeroInt + delta).toString())
                        comCoord = candidatos.firstOrNull { it.temCoordenadas }
                        if (comCoord != null) break

                        val candidatos2 = cnefeRepository.buscarPorCepENumero(endereco.cep, (numeroInt - delta).toString())
                        comCoord = candidatos2.firstOrNull { it.temCoordenadas }
                    }
                }

                if (comCoord != null) {
                    atualizarCoordenadas(proximaRef, comCoord.latitude, comCoord.longitude)
                    encontrouCoord = true
                    return@launch
                }

                // 2. Interpolação entre vizinhos
                if (numeroInt != null) {
                    val interpolado = interpolarCoordenadas(endereco.cep, numeroInt)
                    if (interpolado != null) {
                        atualizarCoordenadas(proximaRef, interpolado.first, interpolado.second)
                        encontrouCoord = true
                        return@launch
                    }
                }

                // 3. Fallback Nominatim
                try {
                    val nominatimResult = nominatimApi.buscarPorEndereco(
                        street = "${endereco.logradouro}, $numero",
                        city = endereco.cidade,
                        state = endereco.estado,
                        postalcode = endereco.cep
                    )
                    if (nominatimResult.isNotEmpty()) {
                        val lat = nominatimResult.first().lat?.toDoubleOrNull()
                        val lng = nominatimResult.first().lon?.toDoubleOrNull()
                        if (lat != null && lng != null) {
                            atualizarCoordenadas(proximaRef, lat, lng)
                            encontrouCoord = true
                            return@launch
                        }
                    }
                } catch (_: Exception) { }

                if (!encontrouCoord) {
                    _state.value = _state.value.copy(
                        isGeocoding = _state.value.isGeocoding - proximaRef
                    )
                }
            } catch (_: Exception) {
                _state.value = _state.value.copy(
                    isGeocoding = _state.value.isGeocoding - proximaRef
                )
            }
        }

        viewModelScope.launch {
            enderecoRepository.salvarEndereco(enderecoComRef.copy(ordem = 1))
        }
    }

    private suspend fun interpolarCoordenadas(cep: String, numero: Int): Pair<Double, Double>? {
        val todos = cnefeRepository.buscarPorCep(cep)
        val comNumero = todos.mapNotNull { dto ->
            val n = dto.numero.toIntOrNull() ?: return@mapNotNull null
            val lat = dto.latitude ?: return@mapNotNull null
            val lng = dto.longitude ?: return@mapNotNull null
            n to (lat to lng)
        }.sortedBy { it.first }

        if (comNumero.isEmpty()) return null
        if (comNumero.size == 1) return comNumero.first().second // único conhecido

        var anterior: Pair<Int, Pair<Double, Double>>? = null
        var proximo: Pair<Int, Pair<Double, Double>>? = null

        for (item in comNumero) {
            if (item.first < numero) anterior = item
            if (item.first > numero && proximo == null) proximo = item
        }

        // Interpolação entre dois vizinhos
        if (anterior != null && proximo != null) {
            val fracao = (numero - anterior.first).toDouble() / (proximo.first - anterior.first).toDouble()
            val lat = anterior.second.first + (proximo.second.first - anterior.second.first) * fracao
            val lng = anterior.second.second + (proximo.second.second - anterior.second.second) * fracao
            return lat to lng
        }

        // Só tem vizinho abaixo → usa o mais próximo (extrapolação ou fallback)
        if (anterior != null) return anterior.second
        // Só tem vizinho acima → usa o mais próximo
        if (proximo != null) return proximo.second

        return null
    }

    private fun atualizarCoordenadas(referencia: Int, lat: Double?, lng: Double?) {
        val paradas = _state.value.paradas.map { p ->
            if (p.referencia == referencia) p.copy(latitude = lat, longitude = lng)
            else p
        }
        _state.value = _state.value.copy(
            paradas = paradas,
            isGeocoding = _state.value.isGeocoding - referencia
        )
    }

    fun cancelarConfirmacao() {
        _state.value = _state.value.copy(
            confirmandoEndereco = null,
            numeroDigitado = ""
        )
    }

    fun removerParada(index: Int) {
        val paradas = _state.value.paradas.toMutableList()
        if (index in paradas.indices) {
            val removida = paradas[index]
            paradas.removeAt(index)
            val reordenadas = paradas.mapIndexed { i, e -> e.copy(ordem = i + 1) }
            _state.value = _state.value.copy(paradas = reordenadas)

            viewModelScope.launch {
                enderecoRepository.deletarEndereco(removida.id)
            }
        }
    }

    fun otimizarRota() {
        val paradas = _state.value.paradas
        if (paradas.size < 2) {
            _state.value = _state.value.copy(
                mensagem = "Adicione pelo menos 2 paradas para otimizar",
                mensagemTipo = MensagemTipo.ERROR
            )
            return
        }

        _state.value = _state.value.copy(isOptimizing = true)

        viewModelScope.launch {
            try {
                // Obtém a posição atual do GPS como origem da rota
                val posicaoAtual = locationService.getUltimaLocalizacao()
                val origem = if (posicaoAtual != null) {
                    OtimizarRotaUseCase.Posicao(
                        latitude = posicaoAtual.latitude,
                        longitude = posicaoAtual.longitude
                    )
                } else null

                val otimizadas = OtimizarRotaUseCase.otimizar(paradas, posicaoAtual = origem)
                _state.value = _state.value.copy(
                    paradas = otimizadas,
                    isOptimizing = false,
                    mensagem = "Rota otimizada com ${otimizadas.size} paradas!" +
                            if (origem != null) " (origem: GPS)" else " (sem GPS)",
                    mensagemTipo = MensagemTipo.SUCCESS
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isOptimizing = false,
                    mensagem = "Erro ao otimizar: ${e.message}",
                    mensagemTipo = MensagemTipo.ERROR
                )
            }
        }
    }

    fun confirmarRota() {
        val paradas = _state.value.paradas
        if (paradas.isEmpty()) {
            _state.value = _state.value.copy(
                mensagem = "Adicione pelo menos uma parada",
                mensagemTipo = MensagemTipo.ERROR
            )
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            try {
                // Rota eterna: reusa o ID da rota ativa existente ou cria um novo
                val rotaExistente = enderecoRepository.getRotaAtiva()
                val rotaId = rotaExistente?.id ?: "rota-${UUID.randomUUID().toString().take(8).uppercase()}"

                // Se já existia, limpa os endereços antigos para reinserir os atuais
                if (rotaExistente != null) {
                    enderecoRepository.limparRota(rotaId)
                }

                val rota = Rota(
                    id = rotaId,
                    nome = "Rota ${java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("pt", "BR")).format(java.util.Date())} - ${paradas.size} parada(s)",
                    paradas = paradas,
                    status = RotaStatus.ATIVA
                )

                enderecoRepository.salvarRota(rota)

                _state.value = _state.value.copy(
                    isSaving = false,
                    mensagem = "Rota salva com sucesso!",
                    mensagemTipo = MensagemTipo.SUCCESS,
                    rotaConfirmadaId = rota.id
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isSaving = false,
                    mensagem = "Erro ao salvar: ${e.message}",
                    mensagemTipo = MensagemTipo.ERROR
                )
            }
        }
    }

    fun limparMensagem() {
        _state.value = _state.value.copy(mensagem = null)
    }
}