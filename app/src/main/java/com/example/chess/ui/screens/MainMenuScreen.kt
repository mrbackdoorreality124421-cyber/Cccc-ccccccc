package com.example.chess.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.model.ChessPosition
import com.example.chess.model.GameMode
import com.example.chess.model.PieceColor
import com.example.chess.ui.ChessViewModel
import com.example.chess.ui.components.ChessBoard2D

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenuScreen(
    viewModel: ChessViewModel,
    onStartGame: () -> Unit,
    onChangeEngine: () -> Unit,
    onOpenPuzzles: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenVisionPuzzle: () -> Unit,
    onOpenBoardEditor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    var showPvBotDialog by remember { mutableStateOf(false) }
    var showHelperBotDialog by remember { mutableStateOf(false) }
    var showFenDialog by remember { mutableStateOf(false) }
    var fenInput by remember { mutableStateOf("") }
    
    var selectedPlayerColor by remember { mutableStateOf(PieceColor.WHITE) }
    var selectedHelperColor by remember { mutableStateOf(PieceColor.WHITE) }
    var helperAutoPlay by remember { mutableStateOf(true) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF10B981)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("♞", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("CHESS FORGE", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp, style = MaterialTheme.typography.titleLarge)
                            Text("ENGINE • PUZZLES • ANALYSIS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            // Header
            item {
                Text(
                    text = "PLAY",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Primary Card: Play vs Stockfish
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPvBotDialog = true }
                        .testTag("menu_play_bot"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF4CAF50), Color(0xFF2E7D32)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SmartToy, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Play vs Stockfish", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Challenge the powerful chess engine", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            
            // Secondary Modes Row
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Custom Board Setup
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenBoardEditor() }
                            .testTag("menu_board_editor"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFFE91E63), modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Board Setup", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    // FEN Load
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showFenDialog = true }
                            .testTag("menu_load_fen"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, tint = Color(0xFF009688), modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Load FEN", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Learning & Analysis
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "LEARN & ANALYZE",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Helper Bot
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showHelperBotDialog = true }
                        .testTag("menu_helper_bot"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF2196F3).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = Color(0xFF2196F3))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Helper Bot", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Engine plays your color & highlights moves", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            
            // Vision Puzzle
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenVisionPuzzle() }
                        .testTag("menu_vision_puzzle"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF673AB7).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ImageSearch, contentDescription = null, tint = Color(0xFF673AB7))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("AI Vision Puzzle", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Scan real boards to get FEN", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Quick Actions Row
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f).clickable { onOpenPuzzles() }.testTag("menu_puzzles"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Extension, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Puzzles", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f).clickable { onOpenHistory() }.testTag("menu_history"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("History", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f).clickable { onOpenSettings() }.testTag("menu_settings"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Settings", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            
            // Engine Info
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChangeEngine() }
                        .testTag("menu_change_engine"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Memory,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Active Engine",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = state.activeEngineName ?: "No Engine Loaded",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Icon(
                            Icons.Default.SwapHoriz,
                            contentDescription = "Change Engine",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }

    // PvP / PvBot Dialog
    if (showPvBotDialog) {
        AlertDialog(
            onDismissRequest = { showPvBotDialog = false },
            title = { Text("Play vs Stockfish", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Choose your color:")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        FilterChip(
                            selected = selectedPlayerColor == PieceColor.WHITE,
                            onClick = { selectedPlayerColor = PieceColor.WHITE },
                            label = { Text("White") }
                        )
                        FilterChip(
                            selected = selectedPlayerColor == PieceColor.BLACK,
                            onClick = { selectedPlayerColor = PieceColor.BLACK },
                            label = { Text("Black") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPvBotDialog = false
                        viewModel.startPlayerVsBot(color = selectedPlayerColor)
                        onStartGame()
                    }
                ) { Text("Start Game") }
            },
            dismissButton = {
                TextButton(onClick = { showPvBotDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Helper Bot Dialog
    if (showHelperBotDialog) {
        AlertDialog(
            onDismissRequest = { showHelperBotDialog = false },
            title = { Text("Helper Bot Settings", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Which color should the bot play?")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        FilterChip(
                            selected = selectedHelperColor == PieceColor.WHITE,
                            onClick = { selectedHelperColor = PieceColor.WHITE },
                            label = { Text("White") }
                        )
                        FilterChip(
                            selected = selectedHelperColor == PieceColor.BLACK,
                            onClick = { selectedHelperColor = PieceColor.BLACK },
                            label = { Text("Black") }
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Auto-play Bot's moves")
                        Switch(
                            checked = helperAutoPlay,
                            onCheckedChange = { helperAutoPlay = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showHelperBotDialog = false
                        viewModel.startHelperBot(
                            botColor = selectedHelperColor,
                            autoPlay = helperAutoPlay
                        )
                        onStartGame()
                    }
                ) { Text("Start Helper Mode") }
            },
            dismissButton = {
                TextButton(onClick = { showHelperBotDialog = false }) { Text("Cancel") }
            }
        )
    }
    
    // FEN Load Dialog
    if (showFenDialog) {
        var fenPreviewError by remember { mutableStateOf<String?>(null) }
        var previewPosition by remember { mutableStateOf<ChessPosition?>(null) }
        
        AlertDialog(
            onDismissRequest = { showFenDialog = false },
            title = { Text("Load Position from FEN", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = fenInput,
                        onValueChange = { 
                            fenInput = it
                            val pos = ChessPosition.fromFen(it.trim())
                            if (pos != null) {
                                previewPosition = pos
                                fenPreviewError = null
                            } else {
                                previewPosition = null
                                fenPreviewError = if (it.isNotBlank()) "Invalid FEN string" else null
                            }
                        },
                        label = { Text("Paste FEN String") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = fenPreviewError != null,
                        supportingText = { fenPreviewError?.let { Text(it) } },
                        singleLine = true
                    )
                    
                    if (previewPosition != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            ChessBoard2D(
                                position = previewPosition!!,
                                selectedSquare = null,
                                legalMoves = emptyList(),
                                onSquareClicked = {},
                                lastMove = null,
                                engineArrowMove = null,
                                orientation = PieceColor.WHITE,
                                
                                theme = state.boardTheme
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pos = previewPosition
                        if (pos != null) {
                            viewModel.startCustomGame(fenInput.trim(), GameMode.ANALYSIS, PieceColor.WHITE)
                            showFenDialog = false
                            onStartGame()
                        }
                    },
                    enabled = previewPosition != null
                ) { Text("Analyze") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val pos = previewPosition
                        if (pos != null) {
                            viewModel.startCustomGame(fenInput.trim(), GameMode.PLAYER_VS_AI, pos.activeColor)
                            showFenDialog = false
                            onStartGame()
                        }
                    },
                    enabled = previewPosition != null
                ) { Text("Play vs AI") }
            }
        )
    }
}
