package br.com.oficjus.drive.domain.usecase

import android.content.Context
import br.com.oficjus.drive.domain.repository.EnderecoRepository
import br.com.oficjus.drive.domain.usecase.WazeNavigator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gerencia o timeout de inatividade do app.
 *
 * Regras:
 * - Rota em execução (ActiveRoute): timeout SUSPENSO
 * - Demais telas: 5 minutos sem interação → timeout
 * - Ao timeout: salva pendências como remanescentes, fecha Waze, encerra app
 */
@Singleton
class SessionTimeoutManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val enderecoRepository: EnderecoRepository
) {

    companion object {
        private const val TIMEOUT_MS = 5 * 60 * 1000L // 5 minutos
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private var timeoutJob: Job? = null
    private var isActiveRoute = false

    /**
     * Informa se a tela atual é a rota em execução.
     * Quando true, o timeout fica suspenso.
     */
    fun setActiveRoute(ativa: Boolean) {
        isActiveRoute = ativa
        if (ativa) {
            cancelarTimeout()
        } else {
            reiniciarTimeout()
        }
    }

    /**
     * Reinicia o timer de timeout.
     * Chamado a cada interação do usuário.
     */
    fun reiniciarTimeout() {
        if (isActiveRoute) return
        cancelarTimeout()
        timeoutJob = scope.launch {
            delay(TIMEOUT_MS)
            executarTimeout()
        }
    }

    /**
     * Cancela o timer sem executar ação.
     */
    fun cancelarTimeout() {
        timeoutJob?.cancel()
        timeoutJob = null
    }

    /**
     * Executa as ações de timeout:
     * 1. Verifica se há rota ativa com pendências
     * 2. Se sim, salva todas como remanescentes e deleta a rota
     * 3. Fecha o Waze
     * 4. Encerra o app
     */
    private suspend fun executarTimeout() {
        // Verifica se há rota ativa com paradas pendentes
        try {
            val rotaAtiva = enderecoRepository.getRotaAtiva()
            if (rotaAtiva != null && rotaAtiva.paradas.isNotEmpty()) {
                // Salva cada parada como remanescente
                rotaAtiva.paradas.forEach { parada ->
                    try { enderecoRepository.salvarRemanescente(parada) } catch (_: Exception) {}
                }
                // Deleta a rota ativa
                try { enderecoRepository.limparRota(rotaAtiva.id) } catch (_: Exception) {}
            }
        } catch (_: Exception) {
            // Se falhar, segue para encerrar mesmo assim
        }

        // Fecha o Waze se estiver aberto
        try { WazeNavigator.matarWaze(context) } catch (_: Exception) {}

        // Encerra o app — chama finishAffinity na MainActivity
        val activity = context as? android.app.Activity
        if (activity != null) {
            activity.finishAffinity()
        } else {
            // Fallback: se não conseguir cast, usa Intent com FLAG_ACTIVITY_CLEAR_TOP
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            intent?.flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                    android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent!!)
            Runtime.getRuntime().exit(0)
        }
    }
}