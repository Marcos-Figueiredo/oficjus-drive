package br.com.oficjus.drive.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder

class BubbleBolhaService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> mostrarBolha(
                intent.getStringExtra(EXTRA_DISTANCIA) ?: "🚗 ---",
                intent.getStringExtra(EXTRA_PARADA) ?: "1/1",
                intent.getBooleanExtra(EXTRA_PODE_AVANCAR, false),
                intent.getBooleanExtra(EXTRA_PODE_VOLTAR, false)
            )
            ACAO_AVANCAR -> sendBroadcast(Intent(ACAO_AVANCAR))
            ACAO_VOLTAR -> sendBroadcast(Intent(ACAO_VOLTAR))
            ACAO_LISTA -> {
                // Reabre o app
                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(launchIntent)
                }
            }
            ACTION_HIDE -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun mostrarBolha(distancia: String, parada: String, podeAvancar: Boolean, podeVoltar: Boolean) {
        try {
            // Intent para o conteúdo expandido da bolha
            val bubbleContentIntent = Intent(this, BubbleBolhaActivity::class.java).apply {
                putExtra(BubbleBolhaActivity.EXTRA_DISTANCIA, distancia)
                putExtra(BubbleBolhaActivity.EXTRA_PARADA, parada)
                putExtra(BubbleBolhaActivity.EXTRA_PODE_AVANCAR, podeAvancar)
                putExtra(BubbleBolhaActivity.EXTRA_PODE_VOLTAR, podeVoltar)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val bubblePendingIntent = PendingIntent.getActivity(
                this, 0, bubbleContentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Intent para voltar ao app ao tocar na notificação
            val voltarIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val voltarPending = PendingIntent.getActivity(
                this, 1, voltarIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Botões de ação
            val avancarIntent = Intent(this, BubbleBolhaService::class.java).apply { action = ACAO_AVANCAR }
            val avancarPending = PendingIntent.getService(this, 2, avancarIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            val voltarAcaoIntent = Intent(this, BubbleBolhaService::class.java).apply { action = ACAO_VOLTAR }
            val voltarAcaoPending = PendingIntent.getService(this, 3, voltarAcaoIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            val listaIntent = Intent(this, BubbleBolhaService::class.java).apply { action = ACAO_LISTA }
            val listaPending = PendingIntent.getService(this, 4, listaIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            // Cria o metadata da bolha
            val bubble = Notification.BubbleMetadata.Builder(
                bubblePendingIntent,
                Icon.createWithResource(this, android.R.drawable.ic_menu_directions)
            )
                .setAutoExpandBubble(true)
                .setSuppressNotification(false) // false: notificação SEMPRE aparece como fallback
                .setDesiredHeight(240)
                .build()

            val notification = Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("OficJus Drive")
                .setContentText("Navegação ativa - $parada • $distancia")
                .setSmallIcon(android.R.drawable.ic_menu_directions)
                .setOngoing(true)
                .setContentIntent(voltarPending)
                .setBubbleMetadata(bubble)
                .addAction(Notification.Action.Builder(null, "◀ Voltar", voltarAcaoPending).build())
                .addAction(Notification.Action.Builder(null, "Avançar ▶", avancarPending).build())
                .addAction(Notification.Action.Builder(null, "Lista", listaPending).build())
                .build()

            startForeground(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            // Fallback: notificação simples com ações
            try {
                val voltarIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val voltarPending = PendingIntent.getActivity(this, 0, voltarIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

                val avancarIntent = Intent(this, BubbleBolhaService::class.java).apply { action = ACAO_AVANCAR }
                val avancarPending = PendingIntent.getService(this, 1, avancarIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

                val voltarAcaoIntent = Intent(this, BubbleBolhaService::class.java).apply { action = ACAO_VOLTAR }
                val voltarAcaoPending = PendingIntent.getService(this, 2, voltarAcaoIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

                val notification = Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("OficJus Drive")
                    .setContentText("Navegação ativa - $parada • $distancia")
                    .setSmallIcon(android.R.drawable.ic_menu_directions)
                    .setOngoing(true)
                    .setContentIntent(voltarPending)
                    .addAction(Notification.Action.Builder(null, "◀ Voltar", voltarAcaoPending).build())
                    .addAction(Notification.Action.Builder(null, "Avançar ▶", avancarPending).build())
                    .build()
                startForeground(NOTIFICATION_ID, notification)
            } catch (e2: Exception) {
                stopSelf()
            }
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Navegação",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val NOTIFICATION_ID = 1002
        const val CHANNEL_ID = "oficjus_drive_navegacao"
        const val ACTION_SHOW = "br.com.oficjus.drive.SHOW_BUBBLE"
        const val ACTION_HIDE = "br.com.oficjus.drive.HIDE_BUBBLE"
        const val ACAO_AVANCAR = "br.com.oficjus.drive.NOTIF_AVANCAR"
        const val ACAO_VOLTAR = "br.com.oficjus.drive.NOTIF_VOLTAR"
        const val ACAO_LISTA = "br.com.oficjus.drive.NOTIF_LISTA"
        const val EXTRA_DISTANCIA = "distancia"
        const val EXTRA_PARADA = "parada"
        const val EXTRA_PODE_AVANCAR = "podeAvancar"
        const val EXTRA_PODE_VOLTAR = "podeVoltar"
    }
}