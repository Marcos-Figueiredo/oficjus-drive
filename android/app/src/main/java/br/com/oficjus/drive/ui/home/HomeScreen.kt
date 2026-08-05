package br.com.oficjus.drive.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.oficjus.drive.ui.login.LoginViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onConstruirRota: () -> Unit,
    onContinuarRota: (rotaId: String) -> Unit,
    onLogout: () -> Unit,
    loginViewModel: LoginViewModel = hiltViewModel(),
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    val loginState by loginViewModel.state.collectAsStateWithLifecycle()
    val homeState by homeViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        homeViewModel.verificarRotaStandby()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "OficJus Drive",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                actions = {
                    IconButton(onClick = {
                        // Esconde a bolha flutuante antes de sair
                        br.com.oficjus.drive.data.service.BolhaOverlay.esconder()
                        loginViewModel.logout()
                        onLogout()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sair",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "🛡️",
                fontSize = 48.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "OficJus Drive",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            loginState.usuario?.let { usuario ->
                Text(
                    text = usuario.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
            } ?: Text(
                text = "App de navegação para diligências",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 48.dp)
            )

            // Botão: Continuar rota standby
            homeState.rotaStandby?.let { rota ->
                Button(
                    onClick = { onContinuarRota(rota.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        "▶ Continuar Rota (${rota.paradas.size} paradas)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(
                onClick = {
                    if (homeState.rotaStandby != null) {
                        // Mostra diálogo de confirmação
                        homeViewModel.mostrarDialogoDescartar()
                    } else {
                        onConstruirRota()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = if (homeState.rotaStandby != null) "Nova Rota (descartar atual)"
                         else "Construir Rota",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { /* TODO: rota ativa */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = MaterialTheme.shapes.medium,
                enabled = false
            ) {
                Text("Rota Ativa", fontSize = 16.sp)
            }

            // Dialog de confirmação ao clicar "Nova Rota" com standby ativa
            if (homeState.mostrarDialogoDescartar) {
                AlertDialog(
                    onDismissRequest = { homeViewModel.cancelarDescarte() },
                    title = { Text("Descartar rota atual?") },
                    text = {
                        Text("Você tem uma rota em andamento com ${homeState.rotaStandby?.paradas?.size ?: 0} paradas. Deseja descartá-la e começar uma nova?")
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            homeViewModel.confirmarDescarte()
                            onConstruirRota()
                        }) {
                            Text("Descartar e começar nova")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { homeViewModel.cancelarDescarte() }) {
                            Text("Cancelar")
                        }
                    }
                )
            }
        }
    }
}