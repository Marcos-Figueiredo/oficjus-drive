package br.com.oficjus.drive.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.oficjus.drive.domain.Rota
import br.com.oficjus.drive.domain.repository.EnderecoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeState(
    val rotaStandby: Rota? = null,
    val mostrarDialogoDescartar: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val enderecoRepository: EnderecoRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    fun verificarRotaStandby() {
        viewModelScope.launch {
            val rota = enderecoRepository.getRotaAtiva()
            _state.value = _state.value.copy(rotaStandby = rota)
        }
    }

    fun mostrarDialogoDescartar() {
        _state.value = _state.value.copy(mostrarDialogoDescartar = true)
    }

    fun confirmarDescarte() {
        viewModelScope.launch {
            _state.value.rotaStandby?.let { rota ->
                enderecoRepository.limparRota(rota.id)
            }
            _state.value = HomeState()
        }
    }

    fun cancelarDescarte() {
        _state.value = _state.value.copy(mostrarDialogoDescartar = false)
    }
}