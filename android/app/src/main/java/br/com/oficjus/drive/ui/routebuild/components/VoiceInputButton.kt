package br.com.oficjus.drive.ui.routebuild.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.util.Locale

/**
 * Botão de microfone que abre o reconhecimento de voz nativo do Android.
 * Retorna o texto reconhecido via onResult.
 */
@Composable
fun VoiceInputButton(
    onResult: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val texto = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (!texto.isNullOrBlank()) {
            onResult(texto)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            abrirMicrofone(context, speechLauncher)
        }
    }

    IconButton(
        onClick = {
            if (hasPermission) {
                abrirMicrofone(context, speechLauncher)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            } else {
                abrirMicrofone(context, speechLauncher)
            }
        },
        modifier = modifier
    ) {
        Icon(
            Icons.Default.Mic,
            contentDescription = "Ditado por voz",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
    }
}

private fun abrirMicrofone(
    context: android.content.Context,
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>
) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale("pt", "BR"))
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Diga o CEP, logradouro ou número do imóvel")
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }
    try {
        launcher.launch(intent)
    } catch (_: Exception) {
        // Reconhecimento de voz não disponível
    }
}