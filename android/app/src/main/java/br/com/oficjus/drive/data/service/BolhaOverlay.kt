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

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var params: WindowManager.LayoutParams? = null
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    fun mostrar(
        context: Context,
        distancia: String,
        parada: String,
        podePular: Boolean,
        onPular: () -> Unit,
        onEntregue: () -> Unit,
        onLista: () -> Unit
    ) {
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

        overlayView?.findViewById<TextView>(
            appContext.resources.getIdentifier("bf_distancia", "id", appContext.packageName)
        )?.text = distancia.replace("🚗 ", "")

        overlayView?.findViewById<TextView>(
            appContext.resources.getIdentifier("bf_parada", "id", appContext.packageName)
        )?.text = "🚗 $parada"

        val btnPular = overlayView?.findViewById<Button>(
            appContext.resources.getIdentifier("bf_pular", "id", appContext.packageName)
        )
        btnPular?.alpha = if (podePular) 1.0f else 0.4f
        btnPular?.setOnClickListener { onPular() }

        overlayView?.findViewById<Button>(
            appContext.resources.getIdentifier("bf_entregue", "id", appContext.packageName)
        )?.setOnClickListener { onEntregue() }

        overlayView?.findViewById<Button>(
            appContext.resources.getIdentifier("bf_lista", "id", appContext.packageName)
        )?.setOnClickListener { onLista() }

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

    fun atualizar(distancia: String, parada: String, podePular: Boolean) {
        val ctx = overlayView?.context ?: return
        overlayView?.findViewById<TextView>(
            ctx.resources.getIdentifier("bf_distancia", "id", ctx.packageName)
        )?.text = distancia.replace("🚗 ", "")

        overlayView?.findViewById<TextView>(
            ctx.resources.getIdentifier("bf_parada", "id", ctx.packageName)
        )?.text = "🚗 $parada"

        overlayView?.findViewById<Button>(
            ctx.resources.getIdentifier("bf_pular", "id", ctx.packageName)
        )?.alpha = if (podePular) 1.0f else 0.4f
    }

    fun esconder() {
        overlayView?.let {
            try { windowManager?.removeView(it) } catch (_: Exception) {}
        }
        overlayView = null
        windowManager = null
    }
}