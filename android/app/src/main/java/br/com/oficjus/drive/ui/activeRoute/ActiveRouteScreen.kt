package br.com.oficjus.drive.ui.activeRoute

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import br.com.oficjus.drive.domain.Endereco
import br.com.oficjus.drive.domain.usecase.WazeNavigator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveRouteScreen(
    rotaId: String,
    onVoltar: () -> Unit,
    viewModel: ActiveRouteViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var mostrarLista by remember { mutableStateOf(false) }

    LaunchedEffect(rotaId) {
        viewModel.carregarRota(rotaId)
    }

    // AlertDialog para selecionar endereço da lista
    if (mostrarLista && state.paradasRestantes.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { mostrarLista = false },
            title = { Text("Selecione o endereço") },
            text = {
                LazyColumn {
                    itemsIndexed(state.paradasRestantes) { index, parada ->
                        val isAtual = index == state.paradaAtualIndex
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isAtual)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.selecionarParadaDaLista(index, context)
                                        mostrarLista = false
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${index + 1}. ${parada.logradouro}",
                                        fontWeight = if (isAtual) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Nº ${parada.numero} - ${parada.bairro}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isAtual) {
                                    Text(
                                        text = "📍",
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { mostrarLista = false }) {
                    Text("Fechar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.rota?.nome ?: "Rota Ativa",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = MaterialTheme.colorScheme.onBackground
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
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            state.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.error ?: "Erro desconhecido",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            state.rota != null -> {
                val rota = state.rota!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Card da parada atual (destaque)
                    val paradaAtual = state.paradaAtual
                    if (paradaAtual != null) {
                        item {
                            val posBolo = state.posicaoNoBolo(paradaAtual.referencia)
                            ParadaAtualCard(
                                endereco = paradaAtual,
                                progresso = state.progresso,
                                posicaoNoBolo = posBolo,
                                totalRestante = state.paradasRestantes.size,
                                distancia = state.distanciaDestino
                            )
                        }

                        item {
                            // Botão Iniciar Navegação
                            Button(
                                onClick = { viewModel.abrirWazeComBolha(context) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Navigation,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Iniciar Navegação",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        item {
                            // Botão Concluir Parada (reordena as restantes)
                            OutlinedButton(
                                onClick = { viewModel.concluirParadaAtual() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Concluir Parada",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Paradas concluídas (se houver)
                    if (state.paradasConcluidas.isNotEmpty()) {
                        item {
                            Text(
                                text = "Concluídas (${state.paradasConcluidas.size})",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        itemsIndexed(
                            items = state.paradasConcluidas,
                            key = { _, item -> "concluida_${item.referencia}" }
                        ) { _, concluida ->
                            ParadaConcluidaItem(endereco = concluida)
                        }
                    }

                    // Paradas restantes
                    item {
                        Text(
                            text = "Paradas (${state.paradasRestantes.size})",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    if (state.paradasRestantes.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "🎉 Rota concluída!",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = {
                                            viewModel.finalizarRota(context)
                                            onVoltar()
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Voltar para Construir Rota")
                                    }
                                }
                            }
                        }
                    } else {
                        itemsIndexed(
                            items = state.paradasRestantes,
                            key = { index, _ -> "restante_$index" }
                        ) { index, parada ->
                            val posBolo = state.posicaoNoBolo(parada.referencia)
                            ParadaListItem(
                                index = index,
                                endereco = parada,
                                isAtual = index == state.paradaAtualIndex,
                                posicaoNoBolo = posBolo,
                                onClick = { viewModel.selecionarParada(index) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParadaConcluidaItem(
    endereco: Endereco
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = endereco.logradouro,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
                Text(
                    text = "Nº ${endereco.numero} - ${endereco.bairro}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun ParadaAtualCard(
    endereco: Endereco,
    progresso: String,
    posicaoNoBolo: Int,
    totalRestante: Int,
    distancia: String = "---"
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Distância no topo + referência no canto
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = distancia,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                if (endereco.referencia > 0) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.background,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${endereco.referencia}",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Text(
                text = "Progresso: $progresso",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Posição no bolo (dinâmica)
            if (posicaoNoBolo > 0) {
                Text(
                    text = "Documento $posicaoNoBolo de $totalRestante no bolo",
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Text(
                text = endereco.logradouro,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Text(
                text = "Nº ${endereco.numero} - ${endereco.bairro}",
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 2.dp)
            )

            Text(
                text = "${endereco.cidade}/${endereco.estado}",
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun ParadaListItem(
    index: Int,
    endereco: Endereco,
    isAtual: Boolean,
    posicaoNoBolo: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isAtual)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            else
                MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Número da ordem + referência de digitação + posição no bolo
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isAtual)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${index + 1}",
                            color = if (isAtual)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
                if (endereco.referencia > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.background,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${endereco.referencia}",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = endereco.logradouro,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isAtual) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 14.sp
                )
                Text(
                    text = "Nº ${endereco.numero} - ${endereco.bairro}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
    }
}