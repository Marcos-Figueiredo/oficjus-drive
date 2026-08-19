package br.com.oficjus.drive.ui.activeRoute

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import br.com.oficjus.drive.domain.Endereco

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveRouteScreen(
    rotaId: String,
    onVoltar: () -> Unit,
    viewModel: ActiveRouteViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(rotaId) {
        viewModel.carregarRota(rotaId, context, onVoltar)
    }

    // Botão voltar: se a rota foi concluída (resumo), finaliza de verdade;
    // senão, apenas navega de volta
    val voltar = {
        if (state.paradasRestantes.isEmpty()) {
            viewModel.finalizarRota(context, onVoltar)
        } else {
            onVoltar()
        }
    }
    BackHandler(onBack = voltar)

    // AlertDialog para selecionar endereço da lista
    if (state.mostrarLista && state.paradasRestantes.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { viewModel.fecharLista() },
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
                                        viewModel.fecharLista()
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
                TextButton(onClick = { viewModel.fecharLista() }) {
                    Text("Fechar")
                }
            }
        )
    }

    // Popup de confirmação de reotimização — aparece quando o usuário escolhe
    // outra parada durante a navegação
    if (state.confirmarReotimizacao) {
        AlertDialog(
            onDismissRequest = { viewModel.fecharReotimizacao() },
            title = { Text("Reotimizar rota?") },
            text = {
                Text(
                    "Deseja reotimizar a rota a partir do endereço selecionado?\n\n" +
                    "As paradas restantes serão reordenadas pela proximidade, " +
                    "desconsiderando entregues e pulados."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.confirmarReotimizacao(context)
                    }
                ) {
                    Text("Sim, reotimizar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.cancelarReotimizacao(context)
                    }
                ) {
                    Text("Não, só navegar")
                }
            }
        )
    }

    // Popup de chegada — substitui a antiga bolha de chegada
    // Aparece quando o GPS detecta que o usuário está a < 50m do destino
    if (state.mostrarChegada) {
        AlertDialog(
            onDismissRequest = { /* não permite fechar — obriga decisão */ },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = null,
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "📍 Você chegou ao seu destino!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Efetuou a entrega?",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.tertiary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.entregueParada(context); viewModel.fecharChegada() },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Sim", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.pularParada(context); viewModel.fecharChegada() },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Não", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {}
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
                    IconButton(onClick = voltar) {
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
        // Bloqueio com mensagem — cobre a tela por 3s, sem permitir interação
        state.mensagem?.let { msg ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .clickable(enabled = false, indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    ) {}
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(32.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Text(
                        text = msg,
                        modifier = Modifier.padding(24.dp),
                        color = when (state.mensagemTipo) {
                            MensagemTipo.ERROR -> MaterialTheme.colorScheme.error
                            MensagemTipo.SUCCESS -> MaterialTheme.colorScheme.primary
                            MensagemTipo.INFO -> MaterialTheme.colorScheme.onSurface
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            return@Scaffold
        }

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
                            // Ir Agora + Ir Depois lado a lado
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Ir Agora: abre o Waze e inicia a navegação
                                Button(
                                    onClick = { viewModel.abrirWazeComBolha(context) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Navigation,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Ir Agora",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                }

                                // Ir Depois: mantém a rota ativa e volta para a tela de construção
                                OutlinedButton(
                                    onClick = onVoltar,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Schedule,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Ir Depois",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                }
                            }
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
                                    Spacer(modifier = Modifier.height(24.dp))

                                    // Cards de resumo: Total, Entregues, Não Entregues
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        ResumoCard(
                                            valor = state.totalInicial,
                                            rotulo = "Total",
                                            cor = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        ResumoCard(
                                            valor = state.entregues,
                                            rotulo = "Entregues",
                                            cor = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        ResumoCard(
                                            valor = state.naoEntregues,
                                            rotulo = "Não Entregues",
                                            cor = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))
                                    Button(
                                        onClick = {
                                            viewModel.finalizarRota(context, onVoltar)
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
private fun ResumoCard(
    valor: Int,
    rotulo: String,
    cor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = cor.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$valor",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = cor
            )
            Text(
                text = rotulo,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
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