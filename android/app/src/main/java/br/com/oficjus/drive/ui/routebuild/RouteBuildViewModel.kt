package br.com.oficjus.drive.ui.routebuild

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.oficjus.drive.domain.Endereco
import br.com.oficjus.drive.domain.Rota
import br.com.oficjus.drive.domain.RotaStatus
import br.com.oficjus.drive.domain.TipoGeocode
import br.com.oficjus.drive.domain.repository.CnefeRepository
import br.com.oficjus.drive.domain.repository.EnderecoRepository
import br.com.oficjus.drive.domain.repository.AuthRepository
import br.com.oficjus.drive.domain.usecase.OtimizarRotaUseCase
import br.com.oficjus.drive.data.local.CnefeUnificadaSync
import br.com.oficjus.drive.data.local.LogradouroCacheSync
import br.com.oficjus.drive.data.local.NumeroCacheSync
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
    val isSaving: Boolean = false,
    val isGeocoding: Set<Int> = emptySet(),
    val confirmandoEndereco: Endereco? = null,
    val numeroDigitado: String = "",
    val mensagem: String? = null,
    val mensagemTipo: MensagemTipo = MensagemTipo.INFO,
    val rotaConfirmadaId: String? = null,
    val enderecoDuplicado: EnderecoDuplicado? = null,
    val syncProgresso: String? = null,
    val syncProgressoPorcentagem: Float = 0f
)

data class EnderecoDuplicado(
    val cep: String,
    val numero: String,
    val logradouro: String,
    val posicaoNoBolo: Int
)

enum class MensagemTipo { INFO, SUCCESS, ERROR }

@HiltViewModel
class RouteBuildViewModel @Inject constructor(
    private val cnefeRepository: CnefeRepository,
    private val enderecoRepository: EnderecoRepository,
    private val nominatimApi: NominatimApi,
    private val locationService: LocationService,
    private val authRepository: AuthRepository,
    private val cacheSync: LogradouroCacheSync,
    private val numeroCacheSync: NumeroCacheSync,
    private val cnefeUnificadaSync: CnefeUnificadaSync
) : ViewModel() {

    private val _state = MutableStateFlow(RouteBuildState())
    val state: StateFlow<RouteBuildState> = _state.asStateFlow()

    private var usuarioEstado: String? = null
    private var usuarioCidade: String? = null

    private var buscarJob: Job? = null
    private var buscarCepJob: Job? = null
    private var otimizarJob: Job? = null

    fun logout(context: android.content.Context? = null) {
        context?.let { br.com.oficjus.drive.domain.usecase.WazeNavigator.matarWaze(it) }
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    init {
        viewModelScope.launch {
            // Carrega remanescentes (pulados/não entregues da rota anterior)
            val remanescentes = enderecoRepository.getRemanescentes()
            if (remanescentes.isNotEmpty()) {
                // Renumera de 1 a N e injeta na lista
                val renumerados = remanescentes.mapIndexed { index, e ->
                    e.copy(referencia = index + 1, ordem = index + 1)
                }
                _state.value = _state.value.copy(paradas = renumerados)
                enderecoRepository.limparRemanescentes()
                mostrarMensagem("${remanescentes.size} remanescente(s) da rota anterior", MensagemTipo.INFO)
            }

            // Carrega estado, cidade e comarca do perfil do usuário
            var session = authRepository.getSession()
            usuarioEstado = session?.estado
            usuarioCidade = session?.cidade

            // Sempre atualiza o perfil para garantir comarcaId atualizado
            try {
                authRepository.refreshProfile()
                session = authRepository.getSession()
                usuarioEstado = session?.estado
                usuarioCidade = session?.cidade
            } catch (_: Exception) { }

            // Fallback final: defaults
            if (usuarioCidade == null) usuarioCidade = "SETE LAGOAS"
            if (usuarioEstado == null) usuarioEstado = "MG"

            // Pega dados da comarca (pode ser nulo se perfil não tiver)
            val comarcaCod = session?.comarcaId ?: ""
            val usage = session?.usage ?: "comarca"

            // Mapeia tribunalId para a pasta no bucket
            val tribunalFolder = when (session?.tribunalId) {
                "06" -> "TRF6"       // TRF6 - 6a Regiao
                "13" -> "TJMG"       // TJMG - Minas Gerais
                else -> "TJMG"        // fallback padrão
            }

            _state.value = _state.value.copy(
                syncProgresso = "Verificando cache...",
                syncProgressoPorcentagem = 0f
            )
            try {
                val estado = usuarioEstado ?: "MG"
                val comarca = comarcaCod.ifBlank { "0672" }

                // Tabela unificada: baixa e popula Room (logradouro_cache + numero_cache)
                if (cnefeUnificadaSync.precisaSincronizar()) {
                    // usage=estado → baixa MG inteiro; usage=comarca → baixa só a comarca
                    val comarcaParam = if (usage == "comarca") comarca else null
                    val textoDownload = if (comarcaParam != null)
                        "Baixando base CNEFE da comarca..."
                    else
                        "Baixando base CNEFE (43 MB)..."

                    _state.value = _state.value.copy(
                        syncProgresso = textoDownload,
                        syncProgressoPorcentagem = 0.1f
                    )
                    cnefeUnificadaSync.sincronizar(estado, comarcaParam, tribunalFolder)

                    val totalLog = cnefeUnificadaSync.contarLogradouros()
                    val totalNum = cnefeUnificadaSync.contarNumeros()

                    _state.value = _state.value.copy(
                        syncProgresso = null,
                        syncProgressoPorcentagem = 1f
                    )
                    mostrarMensagem("Base CNEFE: $totalLog logradouros, $totalNum números", MensagemTipo.SUCCESS)
                } else {
                    // Já tem cache, só verifica tamanho
                    val totalLog = cnefeUnificadaSync.contarLogradouros()
                    val totalNum = cnefeUnificadaSync.contarNumeros()
                    _state.value = _state.value.copy(
                        syncProgresso = null,
                        syncProgressoPorcentagem = 1f
                    )
                    if (totalLog > 0) {
                        mostrarMensagem("Cache CNEFE: $totalLog logradouros, $totalNum números", MensagemTipo.INFO)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("RouteBuildVM", "Cache sync error", e)
                mostrarMensagem("Cache: ${e::class.simpleName} ${e.message?.take(60) ?: ""}", MensagemTipo.ERROR)
            }
        }
    }

    fun onSearchTextChanged(text: String) {
        val apenasDigitos = text.filter { it.isDigit() }
        val textoLimpo = if (apenasDigitos.length == 8) apenasDigitos else text

        _state.value = _state.value.copy(
            searchText = textoLimpo,
            mensagem = null
        )

        if (apenasDigitos.length == 8) {
            buscarCep(apenasDigitos)
            return
        }

        if (text.any { it.isLetter() } && text.length >= 3) {
            buscarJob?.cancel()
            buscarJob = viewModelScope.launch {
                delay(250) // debounce 250ms
                if (text == _state.value.searchText) {
                    buscarLogradouro(text)
                }
            }
        } else {
            _state.value = _state.value.copy(sugestoes = emptyList())
        }
    }

    /**
     * Processa o resultado do ditado por voz.
     * Extrai dígitos, separa CEP (8 dígitos) de número, e preenche automaticamente.
     */
    fun onVoiceResult(texto: String) {
        val apenasDigitos = texto.filter { it.isDigit() }
        val semFormatacao = texto.replace(Regex("[^\\p{L}0-9\\s]"), "").trim()

        if (apenasDigitos.length >= 8) {
            // 8 dígitos ou mais: isola CEP e possível número
            val cep = apenasDigitos.take(8)
            val numero = apenasDigitos.drop(8).take(6)
            _state.value = _state.value.copy(
                searchText = cep,
                sugestoes = emptyList(),
                mensagem = null
            )
            if (numero.isNotBlank()) {
                _state.value = _state.value.copy(numeroDigitado = numero)
            }
            buscarCep(cep)
        } else {
            // Menos de 8 dígitos: busca por logradouro
            // Extrai o número do final do texto (se houver)
            val numero = apenasDigitos.take(6)
            val termo = if (numero.isNotBlank()) {
                // Remove o número do final do texto para buscar só o logradouro
                semFormatacao.replace(Regex("\\s+$numero\\s*$"), "").trim()
            } else semFormatacao

            if (termo.length >= 3) {
                _state.value = _state.value.copy(
                    searchText = termo,
                    sugestoes = emptyList(),
                    mensagem = null
                )
                if (numero.isNotBlank()) {
                    _state.value = _state.value.copy(numeroDigitado = numero)
                }
                viewModelScope.launch {
                    buscarLogradouro(termo)
                }
            } else {
                mostrarMensagem("Não entendi. Tente novamente.", MensagemTipo.ERROR)
            }
        }
    }

    private suspend fun buscarLogradouro(termo: String) {
        _state.value = _state.value.copy(isLoading = true)
        try {
            val resultados = cnefeRepository.buscarPorLogradouro(
                termo,
                cidade = usuarioCidade,
                estado = usuarioEstado
            )

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
                isLoading = false
            )
            mostrarMensagem("Erro ao buscar: ${e.message}", MensagemTipo.ERROR)
        }
    }

    private fun buscarCep(cep: String) {
        // Cancela a busca anterior — evita race condition quando o usuário
        // apaga com backspace e digita outro CEP (registro fantasma)
        buscarCepJob?.cancel()
        buscarCepJob = viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val resultadosCNEFE = cnefeRepository.buscarPorCep(cep)
                // Verifica se o CEP ainda é o mesmo sendo buscado
                if (cep != _state.value.searchText) return@launch
                if (resultadosCNEFE.isNotEmpty()) {
                    val dto = resultadosCNEFE.first()
                    // Confirma que ainda é o mesmo CEP sendo buscado
                    if (cep == _state.value.searchText) {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            searchText = "",
                            confirmandoEndereco = dto,
                            numeroDigitado = ""
                        )
                    }
                } else {
                    if (cep == _state.value.searchText) {
                        mostrarMensagem("CEP não encontrado no CNEFE", MensagemTipo.ERROR)
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false)
                mostrarMensagem("Erro: ${e.message}", MensagemTipo.ERROR)
            }
        }
    }

    fun onSelecionarSugestao(endereco: Endereco) {
        // Preserva o numeroDigitado que veio da voz, se existir
        val numeroAtual = _state.value.numeroDigitado
        _state.value = _state.value.copy(
            searchText = "",
            sugestoes = emptyList(),
            confirmandoEndereco = endereco,
            numeroDigitado = numeroAtual
        )
    }

    fun onNumeroConfirmacaoChanged(numero: String) {
        _state.value = _state.value.copy(numeroDigitado = numero.filter { it.isDigit() })
    }

    fun confirmarNumero() {
        val endereco = _state.value.confirmandoEndereco ?: return
        val numero = _state.value.numeroDigitado

        if (numero.isBlank()) {
            mostrarMensagem("Digite o número do imóvel", MensagemTipo.ERROR)
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
                    posicaoNoBolo = posicao
                )
            )
            return
        }

        adicionarEndereco(endereco, numero)
    }

    fun cancelarDuplicata() {
        _state.value = _state.value.copy(enderecoDuplicado = null)
    }

    private fun adicionarEndereco(endereco: Endereco, numero: String) {

        // Zera coordenadas — serão buscadas em background com CEP+número exato
        val enderecoTemp = endereco.copy(numero = numero, latitude = null, longitude = null)
        val proximaRef = (_state.value.paradas.maxOfOrNull { it.referencia } ?: 0) + 1
        val enderecoComRef = enderecoTemp.copy(referencia = proximaRef, ordem = _state.value.paradas.size + 1)

        // Adiciona no FINAL (não no início) — evita reordenação prematura
        val paradas = _state.value.paradas + listOf(enderecoComRef)
        _state.value = _state.value.copy(
            paradas = paradas,
            confirmandoEndereco = null,
            numeroDigitado = "",
            isGeocoding = _state.value.isGeocoding + proximaRef
        )

        // Busca coordenadas: CNEFE exato → interpolação → Nominatim
        viewModelScope.launch {
            var encontrouCoord = false
            try {
                val numeroInt = numero.toIntOrNull()

                // 1. Tenta CNEFE com número exato
                val resultados = cnefeRepository.buscarPorCepENumero(endereco.cep, numero)
                val comCoord = resultados.firstOrNull { it.temCoordenadas }

                if (comCoord != null) {
                    atualizarCoordenadas(proximaRef, comCoord.latitude, comCoord.longitude, br.com.oficjus.drive.domain.TipoGeocode.EXATO)
                    encontrouCoord = true
                    return@launch
                }

                // 2. Interpolação entre vizinhos (cache local da filha)
                if (numeroInt != null) {
                    val interpolado = interpolarCoordenadas(endereco.cep, numeroInt)
                    if (interpolado != null) {
                        atualizarCoordenadas(proximaRef, interpolado.first, interpolado.second, br.com.oficjus.drive.domain.TipoGeocode.ESTIMADO)
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
                            atualizarCoordenadas(proximaRef, lat, lng, br.com.oficjus.drive.domain.TipoGeocode.ESTIMADO)
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

        // Auto-otimiza após cada adição — reordena por GPS em tempo real
        autoOtimizar()
    }

    /**
     * Reordena as paradas pela distância do GPS (Nearest-Neighbor).
     * Chamado automaticamente após cada endereço adicionado.
     */
    private fun autoOtimizar() {
        val paradas = _state.value.paradas
        if (paradas.size < 3) return  // 2 ou menos: mantém ordem de digitação

        // Se ainda tem endereços sendo geocodificados, não otimiza ainda
        if (_state.value.isGeocoding.isNotEmpty()) return

        // Cancela o job anterior para evitar race condition com geocoding
        otimizarJob?.cancel()
        otimizarJob = viewModelScope.launch {
            delay(300) // debounce: aguarda geocoding terminar antes de reordenar
            val posicaoAtual = locationService.getUltimaLocalizacao()
            val origem = if (posicaoAtual != null) {
                Posicao(posicaoAtual.latitude, posicaoAtual.longitude)
            } else null

            val otimizadas = OtimizarRotaUseCase.otimizar(paradas, posicaoAtual = origem)
            _state.value = _state.value.copy(paradas = otimizadas)
        }
    }

    private suspend fun interpolarCoordenadas(cep: String, numero: Int): Pair<Double, Double>? {
        val todos = cnefeRepository.buscarNumerosPorCep(cep)
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

    private fun atualizarCoordenadas(referencia: Int, lat: Double?, lng: Double?, tipo: TipoGeocode = TipoGeocode.NENHUM) {
        val paradas = _state.value.paradas.map { p ->
            if (p.referencia == referencia) p.copy(latitude = lat, longitude = lng, tipoGeocode = tipo)
            else p
        }
        _state.value = _state.value.copy(
            paradas = paradas,
            isGeocoding = _state.value.isGeocoding - referencia
        )
        // Reordena agora que as coordenadas estão disponíveis
        autoOtimizar()
    }

    fun cancelarConfirmacao() {
        _state.value = _state.value.copy(
            confirmandoEndereco = null,
            numeroDigitado = ""
        )
    }

    fun limparRotaConfirmada() {
        _state.value = _state.value.copy(rotaConfirmadaId = null)
    }

    /**
     * Recarrega remanescentes ao voltar da tela de rota ativa.
     * Remanescentes são pulados/não entregues que ficam reservados para a próxima rota.
     */
    fun refreshState() {
        viewModelScope.launch {
            val remanescentes = enderecoRepository.getRemanescentes()
            if (remanescentes.isNotEmpty()) {
                val renumerados = remanescentes.mapIndexed { index, e ->
                    e.copy(referencia = index + 1, ordem = index + 1)
                }
                _state.value = _state.value.copy(
                    paradas = renumerados,
                    mensagem = null
                )
                enderecoRepository.limparRemanescentes()
            }
            // Se não tem remanescentes, mantém a lista atual (não limpa)
        }
    }

    fun removerParada(index: Int) {
        val paradas = _state.value.paradas.toMutableList()
        if (index in paradas.indices) {
            val removida = paradas[index]
            paradas.removeAt(index)
            val reordenadas = paradas.mapIndexed { i, e -> e.copy(ordem = i + 1) }
            _state.value = _state.value.copy(paradas = reordenadas)

            // Só deleta do banco se o registro foi persistido (id > 0)
            // Paradas em memória (id = 0) não existem no banco — seriam um DELETE WHERE id=0 silencioso
            if (removida.id > 0) {
                viewModelScope.launch {
                    enderecoRepository.deletarEndereco(removida.id)
                }
            }
        }
    }

    fun confirmarRota() {
        val paradasAtuais = _state.value.paradas
        if (paradasAtuais.isEmpty()) {
            mostrarMensagem("Adicione pelo menos uma parada", MensagemTipo.ERROR)
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)

            // Otimiza a rota com o GPS antes de salvar
            val posicaoAtual = locationService.getUltimaLocalizacao()
            val origem = if (posicaoAtual != null) {
                Posicao(posicaoAtual.latitude, posicaoAtual.longitude)
            } else null
            val paradas = if (paradasAtuais.size >= 2) {
                OtimizarRotaUseCase.otimizar(paradasAtuais, posicaoAtual = origem)
            } else paradasAtuais
            _state.value = _state.value.copy(paradas = paradas)

            try {
                // Rota eterna: reusa o ID da rota ativa existente ou cria um novo
                val rotaExistente = enderecoRepository.getRotaAtiva()
                val rotaId = rotaExistente?.id ?: "rota-${UUID.randomUUID().toString().take(8).uppercase()}"

                // Substitui a rota existente pela nova de forma atômica
                // (limpa + salva em uma transação — evita perda de dados em crash)
                val rota = Rota(
                    id = rotaId,
                    nome = "Rota ${java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("pt", "BR")).format(java.util.Date())} - ${paradas.size} parada(s)",
                    paradas = paradas,
                    status = RotaStatus.ATIVA
                )

                enderecoRepository.substituirRota(rotaId, rota)

                _state.value = _state.value.copy(
                    isSaving = false,
                    rotaConfirmadaId = rota.id
                )
                mostrarMensagem("Rota salva com sucesso!", MensagemTipo.SUCCESS)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false)
                mostrarMensagem("Erro ao salvar: ${e.message}", MensagemTipo.ERROR)
            }
        }
    }

    fun limparMensagem() {
        _state.value = _state.value.copy(mensagem = null)
    }

    fun mostrarMensagem(texto: String, tipo: MensagemTipo = MensagemTipo.INFO) {
        _state.value = _state.value.copy(mensagem = texto, mensagemTipo = tipo)
        viewModelScope.launch {
            delay(3000)
            _state.value = _state.value.copy(mensagem = null)
        }
    }
}