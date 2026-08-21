package br.com.oficjus.drive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import br.com.oficjus.drive.domain.usecase.SessionTimeoutManager
import br.com.oficjus.drive.ui.navigation.DriveNavGraph
import br.com.oficjus.drive.ui.theme.OficJusDriveTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var sessionTimeoutManager: SessionTimeoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            OficJusDriveTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // Detecta toque do usuário para resetar o timeout
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        awaitPointerEvent()
                                        sessionTimeoutManager.reiniciarTimeout()
                                    }
                                }
                            }
                    ) {
                        DriveNavGraph(
                            navController = navController,
                            sessionTimeoutManager = sessionTimeoutManager
                        )
                    }
                }
            }
        }
    }
}