package br.com.oficjus.drive.ui.routebuild

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.graphics.Color
import br.com.oficjus.drive.domain.TipoGeocode
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.oficjus.drive.domain.Endereco
import br.com.oficjus.drive.ui.routebuild.components.SmartSearchField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteBuildScreen(
    onRotaConfirmada: (String) -> Unit,
    onLogout: () -> Unit,
    viewModel: RouteBuildViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.rotaConfirmadaId) {
        state.rotaConfirmadaId?.let { rotaId ->
            viewModel.limparRotaConfirmada()
            onRotaConfirmada(rotaId)
        }
    }

    // Recarrega as paradas do banco ao voltar da tela de rota ativa
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // AlertDialog de endereço duplicado
    state.enderecoDuplicado?.let { dup ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelarDuplicata() },
            title = { Text("ℹ️ Endereço repetido") },
            text = {
                val cepFormatado = if (dup.cep.length == 8)
                    "${dup.cep.substring(0, 5)}-${dup.cep.substring(5)}"
                else dup.cep
                val ordinais = arrayOf("", "primeiro", "segundo", "terceiro", "quarto", "quinto",
                    "sexto", "sétimo", "oitavo", "nono", "décimo")
                val ordinal = ordinais.getOrElse(dup.posicaoNoBolo) { "${dup.posicaoNoBolo}º" }
                Text(
                    "Já existe entrega registrada para:\n\n" +
                            "${dup.logradouro}, ${dup.numero} - $cepFormatado\n\n" +
                            "É o $ordinal documento do pacote."
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.cancelarDuplicata() }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Construir Percurso",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                actions = {
                    IconButton(onClick = {
                        // Esconde a bolha flutuante antes de sair
                        br.com.oficjus.drive.data.service.BolhaOverlay.esconder()
                        viewModel.logout()
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
        // Tela de progresso do sync (antes de qualquer conteúdo)
        state.syncProgresso?.let { progresso ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Preparando base de endereços...",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(24.dp))
                    LinearProgressIndicator(
                        progress = { state.syncProgressoPorcentagem },
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = progresso,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return@Scaffold
        }

        // Bloqueio com mensagem — cobre a tela por 3s, sem permitir interação
        state.mensagem?.let { msg ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .clickable(enabled = false, indication = null, interactionSource = remember { MutableInteractionSource() }) {}
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(32.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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

        // Se está confirmando endereço, usa Column com scroll (IME-friendly)
        val confirmando = state.confirmandoEndereco
        if (confirmando != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .navigationBarsPadding()
            ) {
                ConfirmacaoEnderecoCard(
                    endereco = confirmando,
                    numeroDigitado = state.numeroDigitado,
                    onNumeroChanged = viewModel::onNumeroConfirmacaoChanged,
                    onConfirmar = viewModel::confirmarNumero,
                    onCancelar = viewModel::cancelarConfirmacao
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .imePadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Mensagem de status
            val msg = state.mensagem
            if (msg != null) {
                item {
                    val cor = when (state.mensagemTipo) {
                        MensagemTipo.INFO -> MaterialTheme.colorScheme.primary
                        MensagemTipo.SUCCESS -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        MensagemTipo.ERROR -> MaterialTheme.colorScheme.error
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cor.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = msg, modifier = Modifier.padding(12.dp), color = cor, fontSize = 13.sp)
                    }
                }
            }

            // Card de confirmação de endereço
            val confirmando = state.confirmandoEndereco
            if (confirmando != null) {
                item {
                    ConfirmacaoEnderecoCard(
                        endereco = confirmando,
                        numeroDigitado = state.numeroDigitado,
                        onNumeroChanged = viewModel::onNumeroConfirmacaoChanged,
                        onConfirmar = viewModel::confirmarNumero,
                        onCancelar = viewModel::cancelarConfirmacao
                    )
                }
            }

            // Se não está confirmando, mostra campo de busca e lista
            if (confirmando == null) {
                item {
                    SmartSearchField(
                        value = state.searchText,
                        onValueChange = viewModel::onSearchTextChanged,
                        onVoiceResult = viewModel::onVoiceResult,
                        sugestoes = state.sugestoes,
                        onSelecionarSugestao = viewModel::onSelecionarSugestao,
                        isLoading = state.isLoading
                    )
                }

                if (state.paradas.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = viewModel::confirmarRota,
                                enabled = state.paradas.isNotEmpty() && !state.isSaving,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                if (state.isSaving) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                    Spacer(Modifier.width(8.dp))
                                }
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Salvar Rota", fontSize = 14.sp)
                            }
                        }
                    }
                }

                if (state.paradas.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Digite um endereço acima para começar", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        }
                    }
                } else {
                    item {
                        Text("${state.paradas.size} parada(s)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    itemsIndexed(
                        items = state.paradas,
                        key = { index, _ -> "parada_$index" }
                    ) { index, parada ->
                        ParadaCard(
                            index = index,
                            endereco = parada,
                            isGeocoding = state.isGeocoding.contains(parada.referencia),
                            onRemover = { viewModel.removerParada(index) }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun ConfirmacaoEnderecoCard(
    endereco: Endereco,
    numeroDigitado: String,
    onNumeroChanged: (String) -> Unit,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Confirmar Parada",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Campos do endereço (read-only)
            Text(
                text = "Logradouro",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            OutlinedTextField(
                value = endereco.logradouro,
                onValueChange = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Bairro (linha separada)
            Text(
                text = "Bairro",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            OutlinedTextField(
                value = endereco.bairro,
                onValueChange = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    disabledTextColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Cidade/UF (linha separada)
            Text(
                text = "Cidade/UF",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            OutlinedTextField(
                value = "${endereco.cidade}/${endereco.estado}",
                onValueChange = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    disabledTextColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Campo NÚMERO com borda verde limão destacada
            val limeGreen = androidx.compose.ui.graphics.Color(0xFFA3E635)
            val numeroBorderColor = if (numeroDigitado.isBlank())
                limeGreen
            else
                MaterialTheme.colorScheme.outline

            val numeroLabelColor = if (numeroDigitado.isBlank())
                limeGreen
            else
                MaterialTheme.colorScheme.onSurfaceVariant

            Text(
                text = "Número do imóvel",
                color = numeroLabelColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            OutlinedTextField(
                value = numeroDigitado,
                onValueChange = onNumeroChanged,
                placeholder = {
                    Text(
                        "Digite o número",
                        color = limeGreen.copy(alpha = 0.7f)
                    )
                },
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = numeroBorderColor,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(8.dp),
                leadingIcon = null
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCancelar,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancelar")
                }
                Button(
                    onClick = onConfirmar,
                    enabled = numeroDigitado.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Adicionar")
                }
            }
        }
    }
}

@Composable
private fun ParadaCard(
    index: Int,
    endereco: Endereco,
    isGeocoding: Boolean,
    onRemover: () -> Unit
) {
    val temCoordenadas = endereco.temCoordenadas
    val corGeocode = when {
        isGeocoding -> MaterialTheme.colorScheme.primary
        endereco.tipoGeocode == TipoGeocode.EXATO -> Color(0xFF4CAF50)   // verde
        endereco.tipoGeocode == TipoGeocode.ESTIMADO -> Color(0xFFFFC107) // amarelo
        temCoordenadas -> Color(0xFF4CAF50) // fallback: verde (dado legado sem tipo)
        else -> MaterialTheme.colorScheme.error // vermelho: sem coordenada
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (!temCoordenadas && !isGeocoding)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Número sequencial + referência + símbolo, centralizados verticalmente
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxHeight()
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = corGeocode,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (temCoordenadas) "${index + 1}" else "0",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
                if (endereco.referencia > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
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
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isGeocoding) "⏳" else if (temCoordenadas) "✅" else "⚠️",
                    fontSize = 14.sp
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = endereco.logradouro,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                Text(
                    text = "Nº ${endereco.numero} - ${endereco.bairro}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                Text(
                    text = "${endereco.cidade}/${endereco.estado}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
                if (!temCoordenadas && !isGeocoding) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "⚠️ Sem coordenadas — será inserido ao final da rota",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            IconButton(onClick = onRemover) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remover",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}