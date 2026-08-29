package com.example.chess.ui.screens

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.model.*
import com.example.chess.ui.ChessViewModel
import com.example.chess.ui.components.*
import kotlin.math.abs

private val DarkSurfaceBg = Color(0xFF13171D)
private val DarkCardBg = Color(0xFF181D24)
private val DarkCardBorder = Color(0xFF262D38)
private val BrandGreen = Color(0xFF10B981)
private val BrandGreenContainer = Color(0xFF064E3B)
private val ActiveTurnGreen = Color(0xFF22C55E)
private val AmberAssistant = Color(0xFFF59E0B)
private val AmberAssistantBg = Color(0xFF2B2212)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayScreen(
    viewModel: ChessViewModel,
    onBackToMenu: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showNewGameDialog by remember { mutableStateOf(false) }
    var showPgnDialog by remember { mutableStateOf(false) }
    var pgnExportText by remember { mutableStateOf("") }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(DarkSurfaceBg)
    ) {
        val isLandscape = maxWidth > maxHeight * 1.05f || (maxWidth >= 480.dp && maxHeight <= 540.dp)
        val isUltraCompact = maxWidth < 340.dp || maxHeight < 440.dp

        // Only show recommendation arrow for the authorized player/helper bot turn
        val shouldShowArrow = when {
            state.gameMode == GameMode.HELPER_BOT -> state.position.activeColor == state.helperBotColor
            state.gameMode == GameMode.PLAYER_VS_AI -> state.position.activeColor == state.playerColor && state.isAssistantMode
            state.gameMode == GameMode.ANALYSIS -> true
            state.isAssistantMode -> true
            else -> false
        }
        val activeArrow = if (shouldShowArrow) state.engineArrowMove else null

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = if (isUltraCompact) 6.dp else if (isLandscape) 12.dp else 10.dp,
                    vertical = if (isUltraCompact) 4.dp else if (isLandscape) 6.dp else 8.dp
                ),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. TOP HEADER (Brand, Engine title, Assistant pill, Restart, Settings)
            TopPlayHeader(
                state = state,
                isLandscape = isLandscape,
                onBackToMenu = onBackToMenu,
                onToggleAssistant = { viewModel.toggleAssistant() },
                onRestartGame = { showNewGameDialog = true },
                onOpenSettings = onOpenSettings,
                onToggleSound = { viewModel.toggleSound() }
            )

            // 2. MAIN BODY (Side-by-side in landscape / split-screen, or vertical stack in portrait)
            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Column: The Square Chess Board with rounded corners fitting the available height
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkCardBg)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.is3DView) {
                            ChessBoard3D(
                                position = state.position,
                                orientation = state.boardOrientation,
                                selectedSquare = state.selectedSquare,
                                legalMoves = state.legalMovesForSelected,
                                lastMove = state.lastMove,
                                engineArrowMove = activeArrow,
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
                                engineArrowMove = activeArrow,
                                theme = state.boardTheme,
                                onSquareClicked = { viewModel.onSquareClicked(it) }
                            )
                        }
                    }

                    // Right Column: Status Card, Dual Player Cards, Eval & Info Strip
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f),
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Game Status Card
                        StatusCardView(state = state, isCompact = true)

                        // Dual Player Role Cards (Side by side)
                        PlayerRoleCardsRow(state = state, isCompact = true)

                        // Evaluation / Info Strip
                        EvalInfoStrip(state = state, isCompact = true)
                    }
                }
            } else {
                // Portrait / Compact Mode
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Status Card
                    StatusCardView(state = state, isCompact = isUltraCompact)

                    // Dual Player Role Cards
                    PlayerRoleCardsRow(state = state, isCompact = isUltraCompact)

                    // Evaluation Bar
                    if (state.isAssistantMode || state.gameMode == GameMode.PLAYER_VS_AI || state.gameMode == GameMode.HELPER_BOT || state.gameMode == GameMode.ANALYSIS) {
                        EvalInfoStrip(state = state, isCompact = isUltraCompact)
                    }

                    // Centered Chess Board (2D or 3D)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 480.dp)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(DarkCardBg)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.is3DView) {
                            ChessBoard3D(
                                position = state.position,
                                orientation = state.boardOrientation,
                                selectedSquare = state.selectedSquare,
                                legalMoves = state.legalMovesForSelected,
                                lastMove = state.lastMove,
                                engineArrowMove = activeArrow,
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
                                engineArrowMove = activeArrow,
                                theme = state.boardTheme,
                                onSquareClicked = { viewModel.onSquareClicked(it) }
                            )
                        }
                    }

                    // Move History
                    Box(modifier = Modifier.widthIn(max = 480.dp)) {
                        MoveHistoryView(moveHistory = state.moveHistory)
                    }
                }
            }

            // 3. BOTTOM ACTION BAR (New Game in Green, Undo, Modes, Export/Share)
            BottomActionBar(
                state = state,
                isLandscape = isLandscape,
                onNewGame = { showNewGameDialog = true },
                onUndo = { viewModel.undoMove() },
                onOpenModes = { showNewGameDialog = true },
                onExportPgn = {
                    pgnExportText = PgnUtils.exportToPgn(state)
                    showPgnDialog = true
                },
                onFlipBoard = { viewModel.flipBoard() },
                onToggle3D = { viewModel.toggle3DView() }
            )
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

    // New Game / Mode Selection Dialog
    if (showNewGameDialog) {
        var selectedMode by remember { mutableStateOf(state.gameMode) }
        var selectedColor by remember { mutableStateOf(state.playerColor) }

        AlertDialog(
            onDismissRequest = { showNewGameDialog = false },
            title = { Text("Game Modes & Roles", fontWeight = FontWeight.Bold) },
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
                        viewModel.resetGame(
                            mode = selectedMode,
                            playerColor = selectedColor,
                            helperColor = selectedColor,
                            helperAutoPlay = true
                        )
                        showNewGameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen, contentColor = Color.Black)
                ) {
                    Text("Apply & Start", fontWeight = FontWeight.Bold)
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
            title = { Text("Chess PGN Notation", fontWeight = FontWeight.Bold) },
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
                                .background(DarkCardBg, RoundedCornerShape(8.dp))
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
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen, contentColor = Color.Black)
                ) {
                    Text("Share", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPgnDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Engine Alert / Error Dialog
    if (state.engineErrorMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearEngineError() },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Chess Engine Alert",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = state.engineErrorMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFCBD5E1)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearEngineError()
                        onOpenSettings()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen, contentColor = Color.Black)
                ) {
                    Text("Engine Setup", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearEngineError() }) {
                    Text("Dismiss", color = Color.White.copy(alpha = 0.8f))
                }
            },
            containerColor = DarkCardBg,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun TopPlayHeader(
    state: ChessGameState,
    isLandscape: Boolean,
    onBackToMenu: () -> Unit,
    onToggleAssistant: () -> Unit,
    onRestartGame: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleSound: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isLandscape) 42.dp else 48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Branding & Engine Name
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onBackToMenu,
                modifier = Modifier.size(32.dp).testTag("btn_header_back")
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Engine / Bot icon badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = BrandGreenContainer,
                modifier = Modifier.size(if (isLandscape) 32.dp else 36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = "Engine Icon",
                        tint = BrandGreen,
                        modifier = Modifier.size(if (isLandscape) 18.dp else 20.dp)
                    )
                }
            }

            Column {
                Text(
                    text = state.activeEngineName,
                    style = if (isLandscape) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = when (state.gameMode) {
                        GameMode.HELPER_BOT -> "Helper Mode · Auto Plays"
                        GameMode.PLAYER_VS_AI -> "NNUE · Engine Match"
                        GameMode.PLAYER_VS_PLAYER -> "Pass & Play Mode"
                        GameMode.ANALYSIS -> "Full Board Analysis"
                        GameMode.TACTICAL_PUZZLE -> "Tactical Puzzle"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = Color(0xFF94A3B8)
                )
            }
        }

        // Right Actions (Assistant pill, Restart, Sound, Settings)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Assistant Pill Button
            val colorLabel = if (state.playerColor == PieceColor.WHITE) "W" else "B"
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (state.isAssistantMode) AmberAssistantBg else DarkCardBg,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (state.isAssistantMode) AmberAssistant else DarkCardBorder
                ),
                onClick = onToggleAssistant,
                modifier = Modifier.testTag("btn_assistant_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (state.isAssistantMode) AmberAssistant else Color(0xFF94A3B8)
                    )
                    Text(
                        text = "Assistant ($colorLabel)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (state.isAssistantMode) AmberAssistant else Color(0xFF94A3B8)
                    )
                }
            }

            // Quick Restart / New Game
            IconButton(
                onClick = onRestartGame,
                modifier = Modifier.size(32.dp).testTag("btn_header_restart")
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Restart Game",
                    tint = Color(0xFFCBD5E1),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Sound Toggle
            IconButton(
                onClick = onToggleSound,
                modifier = Modifier.size(32.dp).testTag("btn_header_sound")
            ) {
                Icon(
                    if (state.isSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                    contentDescription = "Toggle Sound",
                    tint = if (state.isSoundEnabled) BrandGreen else Color(0xFF64748B),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Settings
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.size(32.dp).testTag("btn_header_settings")
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color(0xFFCBD5E1),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun StatusCardView(state: ChessGameState, isCompact: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = if (isCompact) 8.dp else 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Status Indicator Dot
                val isCheck = state.position.isKingInCheck(state.position.activeColor)
                val dotColor = when {
                    state.status == GameStatus.CHECKMATE -> Color(0xFFEF4444)
                    isCheck -> Color(0xFFEF4444)
                    state.isEngineThinking -> Color(0xFFF59E0B)
                    state.status == GameStatus.IN_PROGRESS -> BrandGreen
                    else -> Color(0xFFF59E0B)
                }

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )

                val statusTitle = when {
                    state.status == GameStatus.CHECKMATE -> "Checkmate"
                    isCheck -> "Check!"
                    state.isEngineThinking -> "Calculating..."
                    state.status == GameStatus.IN_PROGRESS -> {
                        if (state.position.activeColor == PieceColor.WHITE) "White's Turn" else "Black's Turn"
                    }
                    else -> "Game Ready"
                }

                Text(
                    text = statusTitle,
                    style = if (isCompact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Text(
                text = state.puzzleMessage ?: when {
                    state.gameMode == GameMode.HELPER_BOT -> {
                        if (state.position.activeColor == state.helperBotColor) {
                            if (state.helperBotAutoPlay) "Helper Bot is calculating & playing for ${if (state.helperBotColor == PieceColor.WHITE) "White" else "Black"}."
                            else "Helper Bot suggests best move for ${if (state.helperBotColor == PieceColor.WHITE) "White" else "Black"}."
                        } else {
                            "Your turn. Play the move for ${if (state.helperBotColor == PieceColor.WHITE) "Black" else "White"}."
                        }
                    }
                    state.gameMode == GameMode.PLAYER_VS_AI -> if (state.position.activeColor == state.playerColor) "Your turn. Select a piece to move." else "Bot is calculating response."
                    state.position.isKingInCheck(state.position.activeColor) -> "King is under direct attack!"
                    else -> "Select a piece to view legal moves."
                },
                style = MaterialTheme.typography.bodySmall.copy(fontSize = if (isCompact) 11.sp else 12.sp),
                color = Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
private fun PlayerRoleCardsRow(state: ChessGameState, isCompact: Boolean) {
    val isWhiteTurn = state.position.activeColor == PieceColor.WHITE
    val isBlackTurn = state.position.activeColor == PieceColor.BLACK

    val leftIsWhite = state.playerColor == PieceColor.WHITE

    val player1Name = if (state.gameMode == GameMode.PLAYER_VS_AI) {
        if (leftIsWhite) "You" else state.activeEngineName
    } else if (state.gameMode == GameMode.HELPER_BOT) {
        if (state.helperBotColor == PieceColor.WHITE) "Helper Bot" else "You (Manual)"
    } else {
        "White Player"
    }

    val player2Name = if (state.gameMode == GameMode.PLAYER_VS_AI) {
        if (!leftIsWhite) "You" else state.activeEngineName
    } else if (state.gameMode == GameMode.HELPER_BOT) {
        if (state.helperBotColor == PieceColor.BLACK) "Helper Bot" else "You (Manual)"
    } else {
        "Black Player"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Player 1 Card (White)
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCardBg),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                if (isWhiteTurn) ActiveTurnGreen else DarkCardBorder
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = if (isCompact) 6.dp else 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            if (player1Name.contains("Bot") || player1Name.contains("Stockfish") || player1Name.contains("Engine"))
                                Icons.Default.SmartToy else Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFFE2E8F0),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = player1Name,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Active dot
                    if (isWhiteTurn) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(ActiveTurnGreen)
                        )
                    }
                }

                Text(
                    text = "White pieces",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = Color(0xFF94A3B8)
                )
            }
        }

        // Player 2 Card (Black)
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCardBg),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                if (isBlackTurn) ActiveTurnGreen else DarkCardBorder
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = if (isCompact) 6.dp else 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            if (player2Name.contains("Bot") || player2Name.contains("Stockfish") || player2Name.contains("Engine"))
                                Icons.Default.SmartToy else Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFFE2E8F0),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = player2Name,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Active dot
                    if (isBlackTurn) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(ActiveTurnGreen)
                        )
                    }
                }

                Text(
                    text = "Black pieces",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

@Composable
private fun EvalInfoStrip(state: ChessGameState, isCompact: Boolean) {
    val evalText = when {
        state.engineMateIn != null -> "M${abs(state.engineMateIn!!)}"
        else -> {
            val score = state.engineEvaluationCp / 100.0
            if (state.engineEvaluationCp > 0) "+%.2f".format(score) else "%.2f".format(score)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkCardBg)
            .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = if (isCompact) 4.dp else 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Eval",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8)
            )
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF1E293B)
            ) {
                Text(
                    text = evalText,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = BrandGreen
                )
            }
        }

        Text(
            text = when {
                !state.isExternalEngineRunning -> "Engine Offline"
                state.isEngineThinking -> "Thinking..."
                else -> "Ready"
            },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = when {
                !state.isExternalEngineRunning -> Color(0xFFEF4444)
                state.isEngineThinking -> Color(0xFFF59E0B)
                else -> BrandGreen
            }
        )
    }
}

@Composable
private fun BottomActionBar(
    state: ChessGameState,
    isLandscape: Boolean,
    onNewGame: () -> Unit,
    onUndo: () -> Unit,
    onOpenModes: () -> Unit,
    onExportPgn: () -> Unit,
    onFlipBoard: () -> Unit,
    onToggle3D: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isLandscape) 44.dp else 48.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Prominent Green "New Game" Button
        Button(
            onClick = onNewGame,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BrandGreen,
                contentColor = Color(0xFF0F172A)
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = Modifier
                .weight(1.3f)
                .fillMaxHeight()
                .testTag("btn_bottom_new_game")
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("New Game", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
        }

        // Dark Pill "Undo" Button
        FilledTonalButton(
            onClick = onUndo,
            enabled = state.moveHistory.isNotEmpty() && !state.isEngineThinking,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = DarkCardBg,
                contentColor = Color.White,
                disabledContainerColor = DarkCardBg.copy(alpha = 0.5f),
                disabledContentColor = Color(0xFF64748B)
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .testTag("btn_bottom_undo")
        ) {
            Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Undo", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
        }

        // Dark Pill "Modes" Button
        FilledTonalButton(
            onClick = onOpenModes,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = DarkCardBg,
                contentColor = Color.White
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .testTag("btn_bottom_modes")
        ) {
            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Modes", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
        }

        // Dark Tool Square (PGN / Copy / Flip)
        FilledTonalIconButton(
            onClick = onExportPgn,
            shape = RoundedCornerShape(10.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = DarkCardBg,
                contentColor = Color.White
            ),
            modifier = Modifier
                .size(if (isLandscape) 44.dp else 48.dp)
                .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp))
                .testTag("btn_bottom_pgn")
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Export PGN", modifier = Modifier.size(18.dp))
        }

        // Dark Tool Square (Flip)
        FilledTonalIconButton(
            onClick = onFlipBoard,
            shape = RoundedCornerShape(10.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = DarkCardBg,
                contentColor = Color.White
            ),
            modifier = Modifier
                .size(if (isLandscape) 44.dp else 48.dp)
                .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp))
                .testTag("btn_bottom_flip")
        ) {
            Icon(Icons.Default.SwapVert, contentDescription = "Flip Board", modifier = Modifier.size(18.dp))
        }
    }
}
