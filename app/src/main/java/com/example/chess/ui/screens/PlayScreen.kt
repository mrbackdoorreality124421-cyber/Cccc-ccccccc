package com.example.chess.ui.screens

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.model.GameMode
import com.example.chess.model.GameStatus
import com.example.chess.model.PgnUtils
import com.example.chess.model.PieceColor
import com.example.chess.ui.ChessViewModel
import com.example.chess.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayScreen(
    viewModel: ChessViewModel,
    onBackToMenu: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showNewGameDialog by remember { mutableStateOf(false) }
    var showPgnDialog by remember { mutableStateOf(false) }
    var pgnExportText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Navigation header (Back to Main Menu)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = onBackToMenu,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("btn_back_to_menu")
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Main Menu", style = MaterialTheme.typography.labelMedium)
            }

            if (state.gameMode == GameMode.HELPER_BOT) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = state.helperBotAutoPlay,
                        onClick = { viewModel.toggleHelperAutoPlay() },
                        label = { Text(if (state.helperBotAutoPlay) "Auto: ON" else "Auto: PAUSED") },
                        leadingIcon = {
                            Icon(
                                if (state.helperBotAutoPlay) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )

                    if (!state.helperBotAutoPlay) {
                        FilledTonalButton(
                            onClick = { viewModel.triggerManualHelperStep() },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Step Move", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // Top Evaluation Bar (when assistant or PvE is active)
        if (state.isAssistantMode || state.gameMode == GameMode.PLAYER_VS_AI || state.gameMode == GameMode.HELPER_BOT || state.gameMode == GameMode.ANALYSIS) {
            EvaluationBar(
                evalCp = state.engineEvaluationCp,
                mateIn = state.engineMateIn,
                isThinking = state.isEngineThinking
            )
        }

        // Game Status / Puzzle Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    state.status == GameStatus.CHECKMATE -> MaterialTheme.colorScheme.primaryContainer
                    state.position.isKingInCheck(state.position.activeColor) -> MaterialTheme.colorScheme.errorContainer
                    state.gameMode == GameMode.HELPER_BOT -> Color(0xFF2E7D32).copy(alpha = 0.15f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val title = when (state.gameMode) {
                            GameMode.PLAYER_VS_AI -> "Player vs Bot"
                            GameMode.HELPER_BOT -> "Helper Bot (Plays & Guides with Arrows)"
                            GameMode.PLAYER_VS_PLAYER -> "Pass & Play"
                            GameMode.ANALYSIS -> "Analysis Board"
                            GameMode.TACTICAL_PUZZLE -> "Tactical Puzzle"
                        }
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (state.gameMode == GameMode.HELPER_BOT) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )

                        if (state.gameMode == GameMode.PLAYER_VS_AI || state.gameMode == GameMode.HELPER_BOT || state.isAssistantMode) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (state.isStockfishActive) Color(0xFF2E7D32).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = if (state.isStockfishActive) "⚡ ${state.activeEngineName}" else "🤖 ${state.activeEngineName}",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (state.isStockfishActive) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Text(
                        text = state.puzzleMessage ?: state.statusDescription,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (state.position.isKingInCheck(state.position.activeColor)) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface
                    )
                }

                if (state.isEngineThinking) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Thinking...",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Active Chess Board (2D or 3D)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (state.is3DView) {
                ChessBoard3D(
                    position = state.position,
                    orientation = state.boardOrientation,
                    selectedSquare = state.selectedSquare,
                    legalMoves = state.legalMovesForSelected,
                    lastMove = state.lastMove,
                    engineArrowMove = if (state.isAssistantMode) state.engineArrowMove else null,
                    theme = state.boardTheme,
                    onSquareClicked = { viewModel.onSquareClicked(it) }
                )
            } else {
                ChessBoard2D(
                    position = state.position,
                    orientation = state.boardOrientation,
                    selectedSquare = state.selectedSquare,
                    legalMoves = state.legalMovesForSelected,
                    lastMove = state.lastMove,
                    engineArrowMove = if (state.isAssistantMode) state.engineArrowMove else null,
                    theme = state.boardTheme,
                    onSquareClicked = { viewModel.onSquareClicked(it) }
                )
            }
        }

        // Move History Strip
        MoveHistoryView(moveHistory = state.moveHistory)

        // Primary Game Control Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(
                onClick = { viewModel.undoMove() },
                enabled = state.moveHistory.isNotEmpty() && !state.isEngineThinking,
                modifier = Modifier.testTag("btn_undo")
            ) {
                Icon(Icons.Default.Undo, contentDescription = "Undo Move")
            }

            FilledTonalIconButton(
                onClick = { viewModel.redoMove() },
                enabled = state.redoStack.isNotEmpty() && !state.isEngineThinking,
                modifier = Modifier.testTag("btn_redo")
            ) {
                Icon(Icons.Default.Redo, contentDescription = "Redo Move")
            }

            FilledTonalIconButton(
                onClick = { viewModel.flipBoard() },
                modifier = Modifier.testTag("btn_flip")
            ) {
                Icon(Icons.Default.SwapVert, contentDescription = "Flip Board")
            }

            FilledTonalIconToggleButton(
                checked = state.is3DView,
                onCheckedChange = { viewModel.toggle3DView() },
                modifier = Modifier.testTag("btn_3d_toggle")
            ) {
                Icon(Icons.Default.ViewInAr, contentDescription = "3D View Toggle")
            }

            FilledTonalIconToggleButton(
                checked = state.isAssistantMode,
                onCheckedChange = { viewModel.toggleAssistant() },
                modifier = Modifier.testTag("btn_assistant_toggle")
            ) {
                Icon(Icons.Default.Psychology, contentDescription = "Assistant Toggle")
            }

            FilledTonalIconButton(
                onClick = { showNewGameDialog = true },
                modifier = Modifier.testTag("btn_new_game")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "New Game")
            }
        }

        // Bottom Action Buttons: PGN Export and Mode Switcher
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    pgnExportText = PgnUtils.exportToPgn(state)
                    showPgnDialog = true
                },
                modifier = Modifier.weight(1f).testTag("btn_export_pgn")
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export PGN")
            }

            Button(
                onClick = { showNewGameDialog = true },
                modifier = Modifier.weight(1f).testTag("btn_switch_mode")
            ) {
                Icon(Icons.Default.SportsEsports, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Game Mode")
            }
        }
    }

    // Promotion Dialog
    if (state.promotionPending != null) {
        PromotionDialog(
            color = state.position.activeColor,
            onSelectPiece = { viewModel.onPromotionPieceSelected(it) },
            onDismiss = { viewModel.onPromotionDismissed() }
        )
    }

    // New Game Dialog
    if (showNewGameDialog) {
        var selectedMode by remember { mutableStateOf(state.gameMode) }
        var selectedColor by remember { mutableStateOf(state.playerColor) }

        AlertDialog(
            onDismissRequest = { showNewGameDialog = false },
            title = { Text("Start New Game", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Select Game Mode:", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedMode == GameMode.PLAYER_VS_AI,
                            onClick = { selectedMode = GameMode.PLAYER_VS_AI },
                            label = { Text("vs Bot") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedMode == GameMode.HELPER_BOT,
                            onClick = { selectedMode = GameMode.HELPER_BOT },
                            label = { Text("Helper") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedMode == GameMode.PLAYER_VS_PLAYER,
                            onClick = { selectedMode = GameMode.PLAYER_VS_PLAYER },
                            label = { Text("Pass&Play") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (selectedMode == GameMode.PLAYER_VS_AI || selectedMode == GameMode.HELPER_BOT) {
                        Text(if (selectedMode == GameMode.HELPER_BOT) "Bot Plays As:" else "Play As:", style = MaterialTheme.typography.labelLarge)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedColor == PieceColor.WHITE,
                                onClick = { selectedColor = PieceColor.WHITE },
                                label = { Text("White (1st)") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedColor == PieceColor.BLACK,
                                onClick = { selectedColor = PieceColor.BLACK },
                                label = { Text("Black (2nd)") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetGame(mode = selectedMode, playerColor = selectedColor)
                        showNewGameDialog = false
                    }
                ) {
                    Text("Start Game")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewGameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // PGN Dialog
    if (showPgnDialog) {
        AlertDialog(
            onDismissRequest = { showPgnDialog = false },
            title = { Text("PGN Notation", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    SelectionContainer {
                        Text(
                            text = pgnExportText,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .verticalScroll(rememberScrollState())
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, pgnExportText)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Share Chess PGN")
                        context.startActivity(shareIntent)
                        showPgnDialog = false
                    }
                ) {
                    Text("Share PGN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPgnDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
