package br.com.oficjus.drive.ui.wazegate

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import br.com.oficjus.drive.domain.usecase.WazeNavigator

@Composable
fun WazeGateScreen(
    onWazeConfirmado: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var wazeInstalado by remember { mutableStateOf(WazeNavigator.isWazeInstalled(context)) }
    var localizacaoPermitida by remember { mutableStateOf(temPermissaoLocalizacao(context)) }
    var notificacaoPermitida by remember { mutableStateOf(temPermissaoNotificacao(context)) }
    var overlayPermitido by remember { mutableStateOf(temPermissaoOverlay(context)) }
    var audioPermitido by remember { mutableStateOf(temPermissaoAudio(context)) }

    val tudoOk = wazeInstalado && localizacaoPermitida && notificacaoPermitida && overlayPermitido && audioPermitido

    // Launcher para solicitar permissão de localização
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        localizacaoPermitida = isGranted
        if (tudoOk) onWazeConfirmado()
    }

    // Launcher para solicitar permissão de notificação (Android 13+)
    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        notificacaoPermitida = isGranted
        if (tudoOk) onWazeConfirmado()
    }

    // Launcher para solicitar permissão de áudio (Android 6+)
    val audioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        audioPermitido = isGranted
        if (tudoOk) onWazeConfirmado()
    }

    // Verifica quando a tela volta ao foco (ex: após instalar Waze e voltar)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                wazeInstalado = WazeNavigator.isWazeInstalled(context)
                localizacaoPermitida = temPermissaoLocalizacao(context)
                notificacaoPermitida = temPermissaoNotificacao(context)
                overlayPermitido = temPermissaoOverlay(context)
                audioPermitido = temPermissaoAudio(context)
                if (wazeInstalado && localizacaoPermitida && notificacaoPermitida && overlayPermitido && audioPermitido) onWazeConfirmado()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(wazeInstalado, localizacaoPermitida, notificacaoPermitida, overlayPermitido, audioPermitido) {
        if (wazeInstalado && localizacaoPermitida && notificacaoPermitida && overlayPermitido && audioPermitido) onWazeConfirmado()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "🛡️",
                fontSize = 56.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "OficJus Drive",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Para usar o app é necessário:\n\n" +
                        "${if (wazeInstalado) "✅" else "⬜"} Waze instalado\n" +
                        "${if (localizacaoPermitida) "✅" else "⬜"} Permissão de localização (GPS)\n" +
                        "${if (notificacaoPermitida) "✅" else "⬜"} Permissão de notificações\n" +
                        "${if (overlayPermitido) "✅" else "⬜"} Permissão de sobreposição (bolha)\n" +
                        "${if (audioPermitido) "✅" else "⬜"} Permissão de microfone (ditado por voz)\n\n" +
                        "Resolva os itens pendentes e volte ao app.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            if (!wazeInstalado) {
                Button(
                    onClick = {
                        try {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=com.waze")
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Instalar Waze", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            if (!localizacaoPermitida) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("Permitir localização", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            if (!notificacaoPermitida) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            notificacaoPermitida = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Text("Permitir notificações", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            if (!overlayPermitido) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        try {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Permitir sobreposição (bolha)", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            if (!audioPermitido) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            audioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            audioPermitido = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Permitir microfone (ditado)", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun temPermissaoLocalizacao(context: android.content.Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

private fun temPermissaoNotificacao(context: android.content.Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else true
}

private fun temPermissaoOverlay(context: android.content.Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
    } else true
}

private fun temPermissaoAudio(context: android.content.Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    } else true
}