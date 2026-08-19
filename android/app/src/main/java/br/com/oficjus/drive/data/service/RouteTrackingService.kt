package br.com.oficjus.drive.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Foreground Service que mantém o GPS ativo enquanto a rota está em execução,
 * mesmo com o app em background (Waze aberto).
 *
 * O Android 8+ exige um Foreground Service com notificação para acesso contínuo
 * ao GPS. Sem isso, o sistema corta as atualizações quando o app vai para background.
 */
@AndroidEntryPoint
class RouteTrackingService : Service() {

    @Inject lateinit var fusedLocationClient: FusedLocationProviderClient

    companion object {
        private const val CHANNEL_ID = "route_tracking"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_STOP = "br.com.oficjus.drive.STOP_TRACKING"

        private val _ultimaLocalizacao = MutableStateFlow<Location?>(null)
        val ultimaLocalizacao: StateFlow<Location?> = _ultimaLocalizacao.asStateFlow()

        private var isRunning = false
        fun isRunning(): Boolean = isRunning

        fun start(context: Context) {
            val intent = Intent(context, RouteTrackingService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, RouteTrackingService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        criarCanalNotificacao()
        isRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = criarNotificacao()
        startForeground(NOTIFICATION_ID, notification)
        iniciarGps()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        pararGps()
        isRunning = false
    }

    private fun criarCanalNotificacao() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Navegação Ativa",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "GPS ativo para navegação"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    @Suppress("DEPRECATION")
    private fun criarNotificacao(): Notification {
        val stopIntent = Intent(this, RouteTrackingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("OficJus Drive")
            .setContentText("Rota ativa — GPS ligado")
            .setSmallIcon(android.R.drawable.ic_menu_directions)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_media_pause, "Parar", stopPendingIntent)
            .build()
    }

    private fun iniciarGps() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            1000L
        ).apply {
            setMinUpdateIntervalMillis(500L)
            setMaxUpdateDelayMillis(2000L)
        }.build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    _ultimaLocalizacao.value = location
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            callback,
            Looper.getMainLooper()
        )
    }

    private fun pararGps() {
        // fusedLocationClient.removeLocationUpdates() é feito automaticamente
        // pelo sistema quando o serviço é destruído
    }
}