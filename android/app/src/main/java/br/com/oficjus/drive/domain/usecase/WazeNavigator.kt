package br.com.oficjus.drive.domain.usecase

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import br.com.oficjus.drive.domain.Endereco

object WazeNavigator {

    private const val WAZE_PACKAGE = "com.waze"

    fun isWazeInstalled(context: Context): Boolean {
        val wazeIntent = Intent(Intent.ACTION_VIEW, Uri.parse("waze://"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (wazeIntent.resolveActivity(context.packageManager) != null) return true

        val httpsIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://waze.com/ul"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return httpsIntent.resolveActivity(context.packageManager) != null
    }

    fun abrirParaEndereco(context: Context, endereco: Endereco) {
        val coords = "${endereco.latitude},${endereco.longitude}"
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("waze://?ll=$coords&navigate=yes")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * Mata o processo do Waze para garantir que ele não fique
     * ocupando memória ou com tela em branco na próxima abertura.
     */
    fun matarWaze(context: Context) {
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            am.killBackgroundProcesses(WAZE_PACKAGE)
        } catch (_: Exception) { }
    }
}