package br.com.oficjus.drive.data.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView

class BolhaService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var params: WindowManager.LayoutParams? = null
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        when (intent?.action) {
            ACTION_SHOW -> mostrarBolha(
                intent.getStringExtra(EXTRA_DISTANCIA) ?: "🚗 ---",
                intent.getStringExtra(EXTRA_PARADA) ?: "1/1",
                intent.getBooleanExtra(EXTRA_PODE_AVANCAR, false),
                intent.getBooleanExtra(EXTRA_PODE_VOLTAR, false)
            )
            ACTION_HIDE -> {
                esconderBolha()
                stopSelf()
            }
            ACTION_UPDATE -> atualizarBolha(
                intent.getStringExtra(EXTRA_DISTANCIA) ?: "🚗 ---",
                intent.getStringExtra(EXTRA_PARADA) ?: "1/1",
                intent.getBooleanExtra(EXTRA_PODE_AVANCAR, false),
                intent.getBooleanExtra(EXTRA_PODE_VOLTAR, false)
            )
        }
        return START_STICKY
    }

    private fun criarOverlay(
        distancia: String,
        parada: String,
        podeAvancar: Boolean,
        podeVoltar: Boolean
    ) {
        if (overlayView != null) return

        overlayView = LayoutInflater.from(this).inflate(
            resources.getIdentifier("bolha_flutuante_layout", "layout", packageName),
            null
        )

        val tvDistancia = overlayView?.findViewById<TextView>(
            resources.getIdentifier("bf_distancia", "id", packageName)
        )
        tvDistancia?.text = distancia

        val tvParada = overlayView?.findViewById<TextView>(
            resources.getIdentifier("bf_parada", "id", packageName)
        )
        tvParada?.text = parada

        val btnAvancar = overlayView?.findViewById<Button>(
            resources.getIdentifier("bf_avancar", "id", packageName)
        )
        btnAvancar?.alpha = if (podeAvancar) 1.0f else 0.4f

        val btnVoltar = overlayView?.findViewById<Button>(
            resources.getIdentifier("bf_voltar", "id", packageName)
        )
        btnVoltar?.alpha = if (podeVoltar) 1.0f else 0.4f

        // Eventos dos botões → broadcast para o app
        btnAvancar?.setOnClickListener {
            sendBroadcast(Intent(ACAO_AVANCAR))
        }
        btnVoltar?.setOnClickListener {
            sendBroadcast(Intent(ACAO_VOLTAR))
        }
        overlayView?.findViewById<Button>(
            resources.getIdentifier("bf_lista", "id", packageName)
        )?.setOnClickListener {
            sendBroadcast(Intent(ACAO_LISTA))
        }

        // Arrastar
        overlayView?.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params?.x ?: 50
                    initialY = params?.y ?: 200
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (params != null) {
                        params!!.x = initialX + (event.rawX - initialTouchX).toInt()
                        params!!.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(overlayView!!, params!!)
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

        windowManager.addView(overlayView!!, params!!)
    }

    private fun mostrarBolha(distancia: String, parada: String, podeAvancar: Boolean, podeVoltar: Boolean) {
        if (overlayView == null) {
            criarOverlay(distancia, parada, podeAvancar, podeVoltar)
        } else {
            atualizarBolha(distancia, parada, podeAvancar, podeVoltar)
        }
    }

    private fun atualizarBolha(distancia: String, parada: String, podeAvancar: Boolean, podeVoltar: Boolean) {
        overlayView?.findViewById<TextView>(
            resources.getIdentifier("bf_distancia", "id", packageName)
        )?.text = distancia

        overlayView?.findViewById<TextView>(
            resources.getIdentifier("bf_parada", "id", packageName)
        )?.text = parada

        overlayView?.findViewById<Button>(
            resources.getIdentifier("bf_avancar", "id", packageName)
        )?.alpha = if (podeAvancar) 1.0f else 0.4f

        overlayView?.findViewById<Button>(
            resources.getIdentifier("bf_voltar", "id", packageName)
        )?.alpha = if (podeVoltar) 1.0f else 0.4f
    }

    private fun esconderBolha() {
        overlayView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        overlayView = null
    }

    override fun onDestroy() {
        esconderBolha()
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_SHOW = "br.com.oficjus.drive.SHOW_BOLHA"
        const val ACTION_HIDE = "br.com.oficjus.drive.HIDE_BOLHA"
        const val ACTION_UPDATE = "br.com.oficjus.drive.UPDATE_BOLHA"
        const val ACAO_AVANCAR = "br.com.oficjus.drive.BOLHA_AVANCAR"
        const val ACAO_VOLTAR = "br.com.oficjus.drive.BOLHA_VOLTAR"
        const val ACAO_LISTA = "br.com.oficjus.drive.BOLHA_LISTA"

        const val EXTRA_DISTANCIA = "distancia"
        const val EXTRA_PARADA = "parada"
        const val EXTRA_PODE_AVANCAR = "podeAvancar"
        const val EXTRA_PODE_VOLTAR = "podeVoltar"
    }
}