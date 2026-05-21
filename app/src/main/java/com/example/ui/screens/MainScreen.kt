package com.example.ui.screens

import android.app.TimePickerDialog
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.EventEntity
import com.example.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val events by viewModel.events.collectAsState()
    val isGoogleAuthorized by viewModel.isGoogleAuthorized.collectAsState()
    val googleEmail by viewModel.googleEmail.collectAsState()
    val mascotGreeting by viewModel.mascotGreeting.collectAsState()
    val isLoadingMascot by viewModel.isLoadingMascot.collectAsState()
    val syncingState by viewModel.syncingState.collectAsState()

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showGoogleConnectDialog by remember { mutableStateOf(false) }

    // Pulsing animation for elements
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE4D9FF), // Soft lilac
                        Color(0xFFFFF0F5), // Lavender rose
                        Color(0xFFD6E4FF)  // Dreamy blue
                    )
                )
            )
    ) {
        // Floating sparkles cosmic background
        RetroShoujoBackground()

        Scaffold(
            containerColor = Color.Transparent,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddTaskDialog = true },
                    containerColor = ShoujoCuteBorder,
                    contentColor = Color.White,
                    modifier = Modifier
                        .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)
                        .testTag("add_task_fab")
                        .shadow(8.dp, CircleShape, clip = false)
                        .border(2.dp, Color.White, CircleShape),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Adicionar Tarefa Mágica",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // --- Shoujo Vintage Header ---
                ShoujoTitleHeader(pulseScale)

                Spacer(modifier = Modifier.height(16.dp))

                // --- Mascot Bubble (Luna-chan) ---
                LunaMascotBubbleCard(
                    mascotGreeting = mascotGreeting,
                    isLoading = isLoadingMascot,
                    onRefreshGreeting = { viewModel.refreshMascotGreeting() }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // --- Integration Bar (Google Agenda sync) ---
                GoogleSyncStatusCard(
                    isGoogleAuthorized = isGoogleAuthorized,
                    googleEmail = googleEmail,
                    syncingState = syncingState,
                    onConnectClick = { showGoogleConnectDialog = true },
                    onDisconnectClick = { viewModel.disconnectGoogle() },
                    onSyncClick = { viewModel.syncGoogleCalendar() }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // --- Header of the agenda list ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Meus Feitiços do Dia 🌸",
                        color = ShoujoDarkText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )

                    Surface(
                        color = ShoujoLilac.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.clickable {
                            setupDemoData(viewModel)
                        }
                    ) {
                        Text(
                            text = "✨ Carregar Demo",
                            color = ShoujoDarkText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // --- Events List ---
                if (events.isEmpty()) {
                    EmptyStateView()
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(events, key = { it.id }) { event ->
                            EventRowItem(
                                event = event,
                                onCheckToggle = { viewModel.toggleEventCompleted(event) },
                                onDelete = { viewModel.deleteEvent(event) }
                            )
                        }
                    }
                }
            }
        }

        // Dialog: Connect to Google Calendar
        if (showGoogleConnectDialog) {
            GoogleConnectDialog(
                onDismiss = { showGoogleConnectDialog = false },
                onConnect = { token, email ->
                    viewModel.saveOAuthCredentials(token, email)
                    showGoogleConnectDialog = false
                }
            )
        }

        // Dialog: Add Task
        if (showAddTaskDialog) {
            AddTaskDialog(
                onDismiss = { showAddTaskDialog = false },
                onAdd = { title, desc, epoch, category ->
                    viewModel.addLocalEvent(title, desc, epoch, category)
                    showAddTaskDialog = false
                }
            )
        }
    }
}

@Composable
fun ShoujoTitleHeader(pulseScale: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Header top category info tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("⭐", fontSize = 11.sp, color = ShoujoSparkleGold)
                Text(
                    text = "AGENDA MÁGICA 🎀",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = ShoujoDarkText.copy(alpha = 0.7f),
                    fontFamily = FontFamily.SansSerif
                )
                Text("⭐", fontSize = 11.sp, color = ShoujoSparkleGold)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "O que temos",
                fontFamily = FontFamily.Serif,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = Color(0xFF6D28D9), // #6d28d9 purple
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "para hoje?",
                fontFamily = FontFamily.Serif,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                color = Color(0xFFDB2777), // #db2777 pink
                textAlign = TextAlign.Center,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Seu Diário de Compromissos Mágicos",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = ShoujoDarkText.copy(alpha = 0.7f),
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun LunaMascotBubbleCard(
    mascotGreeting: String,
    isLoading: Boolean,
    onRefreshGreeting: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp))
            .background(ShoujoBackgroundCard, RoundedCornerShape(20.dp))
            .border(2.dp, ShoujoPink, RoundedCornerShape(20.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Programmatic Vector luna-chan Kitty Mascot
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(ShoujoLilac.copy(alpha = 0.4f))
                    .clickable { onRefreshGreeting() },
                contentAlignment = Alignment.Center
            ) {
                LunaMascotImage(modifier = Modifier.size(62.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Talk Bubble
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ShoujoBubbleBackground)
                    .border(1.dp, ShoujoCuteBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Luna-chan diz: ✨",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ShoujoPurple,
                        fontFamily = FontFamily.SansSerif
                    )

                    if (isLoading) {
                        CircularProgressIndicator(
                            color = ShoujoCuteBorder,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(12.dp)
                        )
                    } else {
                        // Settings acts as our safe, core-compatible refresh triggers
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Atualizar conselho",
                            tint = ShoujoPurple.copy(alpha = 0.6f),
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { onRefreshGreeting() }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = mascotGreeting,
                    fontSize = 13.sp,
                    color = ShoujoDarkText,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.SansSerif,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun GoogleSyncStatusCard(
    isGoogleAuthorized: Boolean,
    googleEmail: String?,
    syncingState: MainViewModel.SyncState,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onSyncClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        ShoujoBlue.copy(alpha = 0.9f),
                        ShoujoLilac.copy(alpha = 0.8f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Estrela",
                    tint = ShoujoSparkleGold,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (isGoogleAuthorized) "Conexão Estelar Estabelecida! ⭐" else "Conectar com Google Agenda",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ShoujoDarkText
                    )
                    Text(
                        text = if (isGoogleAuthorized) (googleEmail ?: "Google Agenda Ativa") else "Importe seus compromissos reais aqui",
                        fontSize = 11.sp,
                        color = ShoujoDarkText.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isGoogleAuthorized) {
                    IconButton(
                        onClick = onSyncClick,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.White, CircleShape)
                    ) {
                        if (syncingState is MainViewModel.SyncState.Syncing) {
                            CircularProgressIndicator(
                                color = ShoujoPurple,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Sincronizar",
                                tint = ShoujoPurple,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDisconnectClick,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.White.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Desconectar",
                            tint = Color.Red.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Button(
                        onClick = onConnectClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ShoujoPurple,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Conectar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun EventRowItem(
    event: EventEntity,
    onCheckToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val dateText = remember(event.dateTime) {
        val dt = Instant.ofEpochMilli(event.dateTime)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
        dt.format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()))
    }

    val amPmText = remember(event.dateTime) {
        val dt = Instant.ofEpochMilli(event.dateTime)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
        dt.format(DateTimeFormatter.ofPattern("a", Locale.getDefault())).uppercase(Locale.getDefault())
    }

    // Assign appropriate color / icon based on category
    val (catIcon, catColor) = remember(event.category) {
        when (event.category) {
            "Mágico" -> "⭐" to ShoujoPurple
            "Estudo" -> "📖" to ShoujoBlue
            "Amor" -> "💖" to ShoujoPink
            "Lazer" -> "🌸" to ShoujoPink
            "Chá" -> "🍵" to ShoujoLilac
            "Segredo" -> "🔒" to Color(0xFF9D4EDD)
            else -> "✨" to ShoujoLilac
        }
    }

    // Modern glass-backdrop anime-style card with soft elevation shadow or glow
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                clip = false
            )
            .background(
                color = if (event.isCompleted) Color.White.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.72f),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 2.dp,
                color = Color.White.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(10.dp)
            .testTag("event_item_${event.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Elegant vertical status color accent bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(34.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (event.isCompleted) catColor.copy(alpha = 0.4f) else catColor)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Time compartment (min-w-[50px] border-r border-white/50 pr-3)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.width(48.dp)
            ) {
                Text(
                    text = dateText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (event.isCompleted) ShoujoDarkText.copy(alpha = 0.5f) else ShoujoDarkText,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = amPmText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (event.isCompleted) ShoujoDarkText.copy(alpha = 0.4f) else ShoujoDarkText.copy(alpha = 0.6f),
                    fontFamily = FontFamily.SansSerif
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Clean separation divider between Time and Main details
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(28.dp)
                    .background(ShoujoDarkText.copy(alpha = 0.15f))
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Checkbox: Heart styled toggle with 48dp target compatibility area
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (event.isCompleted) ShoujoCuteBorder.copy(alpha = 0.3f) else ShoujoPink.copy(alpha = 0.15f))
                    .clickable { onCheckToggle() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (event.isCompleted) "💖" else "🤍",
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Main Details Block
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = catIcon,
                        fontSize = 14.sp
                    )
                    Text(
                        text = event.title,
                        fontWeight = FontWeight.Bold,
                        color = if (event.isCompleted) ShoujoDarkText.copy(alpha = 0.5f) else ShoujoDarkText,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.SansSerif,
                        textDecoration = if (event.isCompleted) TextDecoration.LineThrough else null,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (!event.description.isBlank()) {
                    Text(
                        text = event.description,
                        fontSize = 11.sp,
                        color = if (event.isCompleted) ShoujoDarkText.copy(alpha = 0.4f) else ShoujoDarkText.copy(alpha = 0.8f),
                        maxLines = 2,
                        textDecoration = if (event.isCompleted) TextDecoration.LineThrough else null,
                        overflow = TextOverflow.Ellipsis,
                        fontFamily = FontFamily.SansSerif
                    )
                }

                if (event.isGoogleEvent) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        color = ShoujoBlue.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "Google Agenda",
                            fontSize = 8.sp,
                            color = ShoujoDarkText,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            // Recycle / delete element
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Excluir",
                    tint = ShoujoPink,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyStateView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Magical Wand or sparkle
        Text(
            text = "✨🧙‍♀️✨",
            fontSize = 44.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Nenhum encargo celestial hoje!",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = ShoujoPurple,
            fontFamily = FontFamily.SansSerif
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Toque no botão rosa '+' no canto para registrar uma tarefa de luz, ou conecte sua Google Agenda para brilhar na tela!",
            fontSize = 12.sp,
            color = ShoujoDarkText.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@Composable
fun GoogleConnectDialog(
    onDismiss: () -> Unit,
    onConnect: (token: String, email: String) -> Unit
) {
    var tokenInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("shoujogirl@gmail.com") }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(24.dp))
                .background(ShoujoBackgroundCard, RoundedCornerShape(24.dp))
                .border(2.dp, ShoujoPurple, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Portal Dimensional da Google Agenda ✨",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = ShoujoPurple,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "A magia conecta com a Google Agenda Real! Escolha uma opção rápida de demonstração ou insira o Token manualmente.",
                    fontSize = 12.sp,
                    color = ShoujoDarkText,
                    textAlign = TextAlign.Center
                )

                // Fast Predefined Demo credentials setup to instantly populate beautiful anime data
                Button(
                    onClick = {
                        onConnect("demo_magic_token_2026", "guerreiramagica@shoujo.com")
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ShoujoPink,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🔮 Ativar Conexão Demo (Rápida)", fontWeight = FontWeight.Bold)
                }

                Divider(color = ShoujoLilac.copy(alpha = 0.5f))

                // Manual fields for OAuth API pasting
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "E-mail da Conta Google",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ShoujoDarkText
                    )
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("seuemail@gmail.com", fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ShoujoPurple,
                            unfocusedBorderColor = ShoujoPink
                        )
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Access Token do Google (Opcional)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ShoujoDarkText
                    )
                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("ya29.a0Axoo...", fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ShoujoPurple,
                            unfocusedBorderColor = ShoujoPink
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, ShoujoPink),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ShoujoPink),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = {
                            val finalToken = if (tokenInput.isNotBlank()) tokenInput else "manual_oauth_token"
                            onConnect(finalToken, emailInput)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ShoujoPurple),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Conectar")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onAdd: (title: String, desc: String, epoch: Long, category: String) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Mágico") }

    // Time Selection
    val calendar = remember { Calendar.getInstance() }
    var selectedHour by remember { mutableStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableStateOf(calendar.get(Calendar.MINUTE)) }

    val categories = listOf("Mágico", "Estudo", "Amor", "Lazer", "Chá", "Segredo")

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hr, min ->
            selectedHour = hr
            selectedMinute = min
        },
        selectedHour,
        selectedMinute,
        true
    )

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(24.dp))
                .background(ShoujoBackgroundCard, RoundedCornerShape(24.dp))
                .border(2.dp, ShoujoCuteBorder, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        text = "🎀 Novo Compromisso Mágico 🎀",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = ShoujoPurple,
                        textAlign = TextAlign.Center
                    )
                }

                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("task_title_input"),
                        label = { Text("Nome da missão / tarefa", fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ShoujoCuteBorder,
                            unfocusedBorderColor = ShoujoLilac
                        )
                    )
                }

                item {
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Anotações da fada (Opcional)", fontSize = 12.sp) },
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ShoujoCuteBorder,
                            unfocusedBorderColor = ShoujoLilac
                        )
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { timePickerDialog.show() }
                            .border(1.dp, ShoujoLilac, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⏰", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Definir Horário Estelar", fontSize = 13.sp, color = ShoujoDarkText)
                        }
                        Text(
                            text = String.format("%02d:%02d", selectedHour, selectedMinute),
                            fontWeight = FontWeight.Bold,
                            color = ShoujoPurple,
                            fontSize = 14.sp
                        )
                    }
                }

                item {
                    Text(
                        text = "Essência da Categoria:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ShoujoDarkText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        textAlign = TextAlign.Start
                    )
                }

                item {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSelected = category == cat
                            Surface(
                                color = if (isSelected) ShoujoCuteBorder else ShoujoLilac.copy(alpha = 0.25f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) ShoujoPurple else ShoujoLilac
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.clickable { category = cat }
                            ) {
                                Text(
                                    text = when (cat) {
                                        "Mágico" -> "⭐ $cat"
                                        "Estudo" -> "📖 $cat"
                                        "Amor" -> "💖 $cat"
                                        "Lazer" -> "🌸 $cat"
                                        "Chá" -> "🍵 $cat"
                                        "Segredo" -> "🔒 $cat"
                                        else -> cat
                                    },
                                    color = if (isSelected) Color.White else ShoujoDarkText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            border = BorderStroke(1.dp, ShoujoPink),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ShoujoPink),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f) // fixed from 1.dp to 1f
                        ) {
                            Text("Cancelar")
                        }

                        Button(
                            onClick = {
                                if (title.isNotBlank()) {
                                    val finalCal = Calendar.getInstance().apply {
                                        set(Calendar.HOUR_OF_DAY, selectedHour)
                                        set(Calendar.MINUTE, selectedMinute)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    onAdd(title, desc, finalCal.timeInMillis, category)
                                }
                            },
                            enabled = title.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = ShoujoPurple),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f) // fixed from 1.dp to 1f
                                .testTag("task_confirm_button")
                        ) {
                            Text("Criar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RetroShoujoBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Twinkling stars programmatically
        val coordinates = listOf(
            Offset(width * 0.15f, height * 0.12f) to 12f,
            Offset(width * 0.82f, height * 0.08f) to 18f,
            Offset(width * 0.08f, height * 0.45f) to 15f,
            Offset(width * 0.90f, height * 0.60f) to 14f,
            Offset(width * 0.25f, height * 0.78f) to 16f,
            Offset(width * 0.78f, height * 0.88f) to 20f,
            Offset(width * 0.50f, height * 0.35f) to 10f
        )

        coordinates.forEach { (pos, radius) ->
            val path = Path().apply {
                moveTo(pos.x, pos.y - radius)
                quadraticTo(pos.x, pos.y, pos.x + radius, pos.y)
                quadraticTo(pos.x, pos.y, pos.x, pos.y + radius)
                quadraticTo(pos.x, pos.y, pos.x - radius, pos.y)
                quadraticTo(pos.x, pos.y, pos.x, pos.y - radius)
                close()
            }
            drawPath(path, color = ShoujoSparkleGold.copy(alpha = 0.5f))
        }

        // Draw dreamy bubbles / soft pastel circles
        val bubbles = listOf(
            Offset(width * 0.30f, height * 0.22f) to 24f,
            Offset(width * 0.70f, height * 0.52f) to 32f,
            Offset(width * 0.10f, height * 0.85f) to 18f,
            Offset(width * 0.85f, height * 0.30f) to 28f
        )

        bubbles.forEach { (pos, r) ->
            drawCircle(
                color = Color.White.copy(alpha = 0.35f),
                radius = r,
                center = pos,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = ShoujoPink.copy(alpha = 0.15f),
                radius = r - 2.dp.toPx(),
                center = pos
            )
        }
    }
}

@Composable
fun LunaMascotImage(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val primaryColor = Color(0xFF6B58A6) // Vintage dark lilac cat color
        val earInnerColor = Color(0xFFFFB3D1) // Pastel pink inner ear
        val crescentColor = Color(0xFFFFE169) // Gold Moon symbol
        val eyeColor = Color(0xFF1F1135) // Deep night eyes
        val sparkleColor = Color.White

        // 1. Draw Ears
        // Left Ear
        val leftEarPath = Path().apply {
            moveTo(w * 0.2f, h * 0.4f)
            lineTo(w * 0.1f, h * 0.1f)
            lineTo(w * 0.45f, h * 0.28f)
            close()
        }
        drawPath(leftEarPath, color = primaryColor)

        val leftEarInnerPath = Path().apply {
            moveTo(w * 0.23f, h * 0.36f)
            lineTo(w * 0.15f, h * 0.15f)
            lineTo(w * 0.40f, h * 0.28f)
            close()
        }
        drawPath(leftEarInnerPath, color = earInnerColor)

        // Right Ear
        val rightEarPath = Path().apply {
            moveTo(w * 0.8f, h * 0.4f)
            lineTo(w * 0.9f, h * 0.1f)
            lineTo(w * 0.55f, h * 0.28f)
            close()
        }
        drawPath(rightEarPath, color = primaryColor)

        val rightEarInnerPath = Path().apply {
            moveTo(w * 0.77f, h * 0.36f)
            lineTo(w * 0.85f, h * 0.15f)
            lineTo(w * 0.60f, h * 0.28f)
            close()
        }
        drawPath(rightEarInnerPath, color = earInnerColor)

        // 2. Draw Cat Face Base Shape
        drawRoundRect(
            color = primaryColor,
            topLeft = Offset(w * 0.15f, h * 0.25f),
            size = Size(w * 0.70f, h * 0.60f),
            cornerRadius = CornerRadius(w * 0.32f, h * 0.30f)
        )

        // 3. Gold Crescent lunar crescent Moon Symbol right on the forehead
        val moonCenter = Offset(w * 0.50f, h * 0.38f)
        val outerRad = w * 0.10f
        val innerRad = w * 0.08f

        val moonPath = Path().apply {
            addArc(
                oval = androidx.compose.ui.geometry.Rect(
                    moonCenter.x - outerRad,
                    moonCenter.y - outerRad,
                    moonCenter.x + outerRad,
                    moonCenter.y + outerRad
                ),
                startAngleDegrees = -120f,
                sweepAngleDegrees = 240f
            )
            addArc(
                oval = androidx.compose.ui.geometry.Rect(
                    moonCenter.x - innerRad + 4.dp.toPx(),
                    moonCenter.y - innerRad,
                    moonCenter.x + innerRad + 4.dp.toPx(),
                    moonCenter.y + innerRad
                ),
                startAngleDegrees = 120f,
                sweepAngleDegrees = -240f
            )
            close()
        }
        drawPath(moonPath, color = crescentColor)

        // 4. Large Sparkling anime eyes
        val eyeY = h * 0.55f
        val eyeW = w * 0.12f
        val eyeH = h * 0.18f

        // Left Eye
        drawOval(
            color = eyeColor,
            topLeft = Offset(w * 0.28f, eyeY),
            size = Size(eyeW, eyeH)
        )
        // Eye Sparkle Left
        drawCircle(
            color = sparkleColor,
            radius = w * 0.035f,
            center = Offset(w * 0.31f, eyeY + eyeH * 0.3f)
        )
        drawCircle(
            color = sparkleColor,
            radius = w * 0.015f,
            center = Offset(w * 0.36f, eyeY + eyeH * 0.7f)
        )

        // Right Eye
        drawOval(
            color = eyeColor,
            topLeft = Offset(w * 0.60f, eyeY),
            size = Size(eyeW, eyeH)
        )
        // Eye Sparkle Right
        drawCircle(
            color = sparkleColor,
            radius = w * 0.035f,
            center = Offset(w * 0.63f, eyeY + eyeH * 0.3f)
        )
        drawCircle(
            color = sparkleColor,
            radius = w * 0.015f,
            center = Offset(w * 0.68f, eyeY + eyeH * 0.7f)
        )

        // Cheek Blushes
        drawCircle(
            color = Color(0xFFFF6B81).copy(alpha = 0.45f),
            radius = w * 0.05f,
            center = Offset(w * 0.23f, h * 0.74f)
        )
        drawCircle(
            color = Color(0xFFFF6B81).copy(alpha = 0.45f),
            radius = w * 0.05f,
            center = Offset(w * 0.77f, h * 0.74f)
        )

        // Nose
        drawCircle(
            color = earInnerColor,
            radius = w * 0.02f,
            center = Offset(w * 0.50f, h * 0.72f)
        )

        // Cute smiling mouth curve
        val mouthPath = Path().apply {
            moveTo(w * 0.44f, h * 0.76f)
            quadraticTo(w * 0.47f, h * 0.81f, w * 0.50f, h * 0.77f)
            quadraticTo(w * 0.53f, h * 0.81f, w * 0.56f, h * 0.76f)
        }
        drawPath(
            mouthPath,
            color = eyeColor,
            style = Stroke(width = 2.dp.toPx())
        )

        // Whiskers
        drawLine(eyeColor, Offset(w * 0.22f, h * 0.72f), Offset(w * 0.05f, h * 0.70f), strokeWidth = 1.5.dp.toPx())
        drawLine(eyeColor, Offset(w * 0.21f, h * 0.76f), Offset(w * 0.03f, h * 0.77f), strokeWidth = 1.5.dp.toPx())
        drawLine(eyeColor, Offset(w * 0.78f, h * 0.72f), Offset(w * 0.95f, h * 0.70f), strokeWidth = 1.5.dp.toPx())
        drawLine(eyeColor, Offset(w * 0.79f, h * 0.76f), Offset(w * 0.97f, h * 0.77f), strokeWidth = 1.5.dp.toPx())
    }
}

private fun setupDemoData(viewModel: MainViewModel) {
    val demoTasks = listOf(
        Triple("Café com Luna Nya 🍵", "Tomar chá de jasmim e dar doces para a Luna", "Chá"),
        Triple("Reunião no Templo da Lua 🌙", "Sincronizar tarefas de luz cósmica", "Mágico"),
        Triple("Estudo de Runas Mágicas 📖", "Fazer fichamento de feitiços de proteção", "Estudo"),
        Triple("Passeio de bicicleta no parque 🌸", "Se exercitar para acumular pontos de energia", "Lazer"),
        Triple("Segredo Oculto de Vênus 🕵️‍♀️", "Comprar o presente surpresa da guerreira", "Segredo")
    )

    demoTasks.forEachIndexed { idx, (title, desc, cat) ->
        val hour = 8 + (idx * 2)
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        viewModel.addLocalEvent(title, desc, cal.timeInMillis, cat)
    }
}
