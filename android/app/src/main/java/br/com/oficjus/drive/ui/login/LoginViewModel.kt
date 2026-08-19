package br.com.oficjus.drive.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.oficjus.drive.domain.model.Usuario
import br.com.oficjus.drive.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val usuario: Usuario? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    init {
        // Verifica se já tem sessão ativa
        viewModelScope.launch {
            val session = authRepository.getSession()
            if (session != null) {
                _state.value = _state.value.copy(
                    isLoggedIn = true,
                    usuario = session
                )
            }
        }
    }

    fun onEmailChanged(email: String) {
        _state.value = _state.value.copy(email = email, error = null)
    }

    fun onPasswordChanged(password: String) {
        _state.value = _state.value.copy(password = password, error = null)
    }

    fun login() {
        val email = _state.value.email.trim()
        val password = _state.value.password

        if (email.isBlank()) {
            _state.value = _state.value.copy(error = "Digite seu email")
            return
        }
        if (password.isBlank()) {
            _state.value = _state.value.copy(error = "Digite sua senha")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            val result = authRepository.login(email, password)

            result.fold(
                onSuccess = { usuario ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        usuario = usuario
                    )
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message ?: "Falha no login"
                    )
                }
            )
        }
    }

    fun logout(context: android.content.Context? = null) {
        // Garante que a bolha flutuante seja removida ao sair
        br.com.oficjus.drive.data.service.BolhaOverlay.esconder()
        context?.let { br.com.oficjus.drive.domain.usecase.WazeNavigator.matarWaze(it) }
        viewModelScope.launch {
            authRepository.logout()
            _state.value = LoginState()
        }
    }
}