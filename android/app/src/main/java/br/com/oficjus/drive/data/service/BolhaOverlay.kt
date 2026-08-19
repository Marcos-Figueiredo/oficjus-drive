package br.com.oficjus.drive.data.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView

@SuppressLint("StaticFieldLeak")
object BolhaOverlay {

    // Mutex para evitar múltiplos overlays em cliques rápidos (race condition)
    private val mutex = Any()

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var params: WindowManager.LayoutParams? = null
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    // ── Referências cacheadas das Views (evita getIdentifier a cada atualização) ──
    private var txtDistancia: TextView? = null
    private var txtParada: TextView? = null
    private var btnEsquerdo: Button? = null
    private var btnDireito: Button? = null
    private var btnSim: Button? = null
    private var btnNao: Button? = null
    private var botoesChegada: android.view.View? = null
    private var cardAtual: android.view.View? = null
    private var cardProxima: android.view.View? = null
    private var txtAtualEndereco: TextView? = null
    private var txtProximaEndereco: TextView? = null

    // Últimos parâmetros para restaurar a bolha
    private var ultimoContext: Context? = null
    private var ultimaDistancia: String = ""
    private var ultimaParada: String = ""
    private var ultimoPodePular: Boolean = false
    private var ultimoOnPular: (() -> Unit)? = null
    private var ultimoOnLista: (() -> Unit)? = null

    // Callbacks para modo chegada
    private var onChegadaSim: (() -> Unit)? = null
    private var onChegadaNao: (() -> Unit)? = null
    private var modoChegada: Boolean = false

    fun mostrar(
        context: Context,
        distancia: String,
        parada: String,
        podePular: Boolean,
        onPular: () -> Unit,
        onLista: () -> Unit
    ) {
        synchronized(mutex) {
            // Guarda parâmetros para restaurar depois
            ultimoContext = context
            ultimaDistancia = distancia
            ultimaParada = parada
            ultimoPodePular = podePular
            ultimoOnPular = onPular
            ultimoOnLista = onLista

            if (overlayView != null) {
                atualizar(distancia, parada, podePular)
                return
            }

            val appContext = context.applicationContext
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(appContext)) return

            windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

            overlayView = LayoutInflater.from(appContext).inflate(
                appContext.resources.getIdentifier("bolha_flutuante_layout", "layout", appContext.packageName),
                null
            )

            // Cacheia as referências para evitar getIdentifier() a cada atualização
            txtDistancia = overlayView?.findViewById<TextView>(
                appContext.resources.getIdentifier("bf_distancia", "id", appContext.packageName)
            )
            txtParada = overlayView?.findViewById<TextView>(
                appContext.resources.getIdentifier("bf_parada", "id", appContext.packageName)
            )
            btnEsquerdo = overlayView?.findViewById<Button>(
                appContext.resources.getIdentifier("bf_esquerdo", "id", appContext.packageName)
            )
            btnDireito = overlayView?.findViewById<Button>(
                appContext.resources.getIdentifier("bf_direito", "id", appContext.packageName)
            )
            cardAtual = overlayView?.findViewById(
                appContext.resources.getIdentifier("bf_card_atual", "id", appContext.packageName)
            )
            cardProxima = overlayView?.findViewById(
                appContext.resources.getIdentifier("bf_card_proxima", "id", appContext.packageName)
            )
            txtAtualEndereco = overlayView?.findViewById<TextView>(
                appContext.resources.getIdentifier("bf_atual_endereco", "id", appContext.packageName)
            )
            txtProximaEndereco = overlayView?.findViewById<TextView>(
                appContext.resources.getIdentifier("bf_proxima_endereco", "id", appContext.packageName)
            )
            botoesChegada = overlayView?.findViewById(
                appContext.resources.getIdentifier("bf_botoes_chegada", "id", appContext.packageName)
            )
            btnSim = overlayView?.findViewById<Button>(
                appContext.resources.getIdentifier("bf_sim", "id", appContext.packageName)
            )
            btnNao = overlayView?.findViewById<Button>(
                appContext.resources.getIdentifier("bf_nao", "id", appContext.packageName)
            )

            txtDistancia?.text = distancia.replace("🚗 ", "")
            txtParada?.text = "🚗 $parada"

            // Cards visíveis apenas no modo chegada
            cardAtual?.visibility = android.view.View.GONE
            cardProxima?.visibility = android.view.View.GONE

            val btnEsquerdo = overlayView?.findViewById<Button>(
                appContext.resources.getIdentifier("bf_esquerdo", "id", appContext.packageName)
            )
            btnEsquerdo?.text = "☰"
            btnEsquerdo?.setOnClickListener { onLista() }
            btnEsquerdo?.alpha = 1.0f
            btnEsquerdo?.isEnabled = true

            btnDireito?.alpha = if (podePular) 1.0f else 0.4f
            btnDireito?.isEnabled = podePular
            btnDireito?.text = "⏭️"
            btnDireito?.setOnClickListener { if (podePular) onPular() }

            overlayView?.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params?.x ?: 50
                        initialY = params?.y ?: 200
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (params != null && windowManager != null) {
                            params!!.x = initialX + (event.rawX - initialTouchX).toInt()
                            params!!.y = initialY + (event.rawY - initialTouchY).toInt()
                            windowManager!!.updateViewLayout(overlayView!!, params!!)
                        }
                        true
                    }
                    else -> false
                }
            }

            val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE

            params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                flag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 50
                y = 200
            }

            windowManager?.addView(overlayView!!, params!!)
        }
    }

    fun atualizar(distancia: String, parada: String, podePular: Boolean) {
        // Salva os últimos valores — restaurarModoNavegacao() usa eles
        ultimaDistancia = distancia
        ultimaParada = parada
        ultimoPodePular = podePular

        // Usa referências cacheadas — sem getIdentifier() em runtime
        txtDistancia?.text = distancia.replace("🚗 ", "")
        txtParada?.text = "🚗 $parada"
        btnDireito?.alpha = if (podePular) 1.0f else 0.4f
        btnDireito?.isEnabled = podePular
    }

    /** Salva os últimos valores sem atualizar a UI (usado pelo ViewModel). */
    fun salvarUltimosValores(distancia: String, parada: String, podePular: Boolean) {
        ultimaDistancia = distancia
        ultimaParada = parada
        ultimoPodePular = podePular
    }

    fun esconder() {
        synchronized(mutex) {
            overlayView?.let {
                try { windowManager?.removeView(it) } catch (_: Exception) {}
            }
            overlayView = null
            windowManager = null
            txtDistancia = null
            txtParada = null
            btnEsquerdo = null
            btnDireito = null
            btnSim = null
            btnNao = null
            botoesChegada = null
            cardAtual = null
            cardProxima = null
            txtAtualEndereco = null
            txtProximaEndereco = null
            // NÃO limpa o estado salvo — é necessário para restaurar()
        }
    }

    /**
     * Limpa o estado salvo da bolha. Chamado quando a rota é finalizada
     * ou o ViewModel destruído — evita que a bolha reapareça.
     */
    fun limparEstado() {
        synchronized(mutex) {
            ultimoContext = null
            ultimoOnPular = null
            ultimoOnLista = null
            onChegadaSim = null
            onChegadaNao = null
            modoChegada = false
        }
    }

    fun isAtiva(): Boolean = overlayView != null

    /**
     * Transforma a bolha em modo "chegada" — pergunta Sim/Não sobre a entrega.
     * A bolha continua sobre o mapa do Waze (TYPE_APPLICATION_OVERLAY).
     * Mostra card com o endereço atual, botões de confirmação e card da próxima.
     */
    fun mostrarChegada(
        onSim: () -> Unit,
        onNao: () -> Unit,
        enderecoAtual: String = "",
        proximaEntrega: String = ""
    ) {
        modoChegada = true
        onChegadaSim = onSim
        onChegadaNao = onNao

        // Usa referências cacheadas
        txtParada?.text = "📍 Chegou!"
        txtDistancia?.visibility = android.view.View.GONE

        // Esconde botões laterais (☰ e ⏭️)
        btnEsquerdo?.visibility = android.view.View.GONE
        btnDireito?.visibility = android.view.View.GONE

        // Card da entrega atual
        cardAtual?.visibility = if (enderecoAtual.isNotBlank()) android.view.View.VISIBLE else android.view.View.GONE
        txtAtualEndereco?.text = enderecoAtual

        // Botões Sim/Não
        botoesChegada?.visibility = android.view.View.VISIBLE
        btnSim?.setOnClickListener { onSim() }
        btnNao?.setOnClickListener { onNao() }

        // Card da próxima entrega
        val textoProxima = if (proximaEntrega.isNotBlank()) proximaEntrega else ""
        cardProxima?.visibility = if (textoProxima.isNotBlank()) android.view.View.VISIBLE else android.view.View.GONE
        txtProximaEndereco?.text = textoProxima
    }

    fun isModoChegada(): Boolean = modoChegada

    /**
     * Restaura a bolha flutuante depois que a bolha de chegada for fechada.
     */
    fun restaurar() {
        val ctx = ultimoContext ?: return
        val onPular = ultimoOnPular ?: return
        val onLista = ultimoOnLista ?: return
        modoChegada = false
        mostrar(ctx, ultimaDistancia, ultimaParada, ultimoPodePular, onPular, onLista)
    }

    /**
     * Restaura o modo navegação na própria bolha (não esconde).
     * Troca os botões de volta para ☰ e ⏭️.
     */
    fun restaurarModoNavegacao() {
        modoChegada = false
        
        txtParada?.text = "🚗 $ultimaParada"
        txtDistancia?.visibility = android.view.View.VISIBLE
        txtDistancia?.text = ultimaDistancia.replace("🚗 ", "")

        // Restaura botões laterais
        btnEsquerdo?.visibility = android.view.View.VISIBLE
        btnEsquerdo?.text = "☰"
        btnEsquerdo?.setOnClickListener { ultimoOnLista?.invoke() }
        btnEsquerdo?.alpha = 1.0f
        btnEsquerdo?.isEnabled = true

        btnDireito?.visibility = android.view.View.VISIBLE
        btnDireito?.text = "⏭️"
        btnDireito?.alpha = if (ultimoPodePular) 1.0f else 0.4f
        btnDireito?.isEnabled = ultimoPodePular
        btnDireito?.setOnClickListener { if (ultimoPodePular) ultimoOnPular?.invoke() }

        // Esconde cards e botões de chegada
        cardAtual?.visibility = android.view.View.GONE
        cardProxima?.visibility = android.view.View.GONE
        botoesChegada?.visibility = android.view.View.GONE
    }
}