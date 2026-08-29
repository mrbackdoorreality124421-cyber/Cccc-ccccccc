package com.example.chess.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.model.ChessPosition
import com.example.chess.model.GameMode
import com.example.chess.model.PieceColor
import com.example.chess.ui.ChessViewModel
import com.example.chess.ui.components.ChessBoard2D

/**
 * Full-featured 2-Step FEN Position Load Dialog.
 *
 * Step 1: Input & validate FEN string with interactive 2D board preview & position badges.
 * Step 2: Choose between:
 *   1. Play Against Bot (with White/Black selection, respects side-to-move, auto first bot move).
 *   2. Bot Helper (friend/assistant mode providing move arrows without auto-playing).
 */
@Composable
fun FenLoadDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    viewModel: ChessViewModel,
    onFenLoaded: () -> Unit
) {
    if (!isOpen) return

    val state by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    var step by remember { mutableStateOf(1) }
    var fenInput by remember { mutableStateOf("") }
    var validatedPosition by remember { mutableStateOf<ChessPosition?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }

    var selectedMode by remember { mutableStateOf(GameMode.PLAYER_VS_AI) }
    var selectedPlayerColor by remember { mutableStateOf(PieceColor.WHITE) }
    var selectedHelperColor by remember { mutableStateOf(PieceColor.WHITE) }
    var helperAutoPlay by remember { mutableStateOf(true) }

    fun validateFen(input: String): Boolean {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            validationError = "Please enter or paste a FEN string."
            validatedPosition = null
            return false
        }
        val pos = ChessPosition.fromFen(trimmed)
        return if (pos != null) {
            validatedPosition = pos
            selectedPlayerColor = pos.activeColor
            selectedHelperColor = pos.activeColor
            validationError = null
            true
        } else {
            validatedPosition = null
            validationError = "Invalid FEN: Must have 6 standard parts with valid kings for both sides."
            false
        }
    }

    if (step == 1) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Terminal, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Load Position from FEN", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Paste a standard 6-field FEN position string to setup the board.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = fenInput,
                        onValueChange = {
                            fenInput = it
                            if (it.isNotBlank()) validateFen(it) else {
                                validatedPosition = null
                                validationError = null
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("fen_input"),
                        label = { Text("FEN String") },
                        placeholder = { Text("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1") },
                        isError = validationError != null,
                        supportingText = {
                            validationError?.let {
                                Text(it, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        trailingIcon = {
                            Row {
                                if (fenInput.isNotEmpty()) {
                                    IconButton(onClick = { fenInput = ""; validatedPosition = null; validationError = null }) {
                                        Icon(Icons.Default.Clear, "Clear")
                                    }
                                }
                                IconButton(onClick = {
                                    clipboardManager.getText()?.text?.let { clipText ->
                                        fenInput = clipText.trim()
                                        validateFen(fenInput)
                                    }
                                }) {
                                    Icon(Icons.Default.ContentPaste, "Paste")
                                }
                            }
                        },
                        maxLines = 3
                    )

                    // Position Preview & Badges
                    validatedPosition?.let { pos ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Side to Move Badge
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (pos.activeColor == PieceColor.WHITE) Color(0xFFF1F5F9) else Color(0xFF1E293B)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                if (pos.activeColor == PieceColor.WHITE) "♙ White to move" else "♟ Black to move",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (pos.activeColor == PieceColor.WHITE) Color.Black else Color.White
                                            )
                                        }
                                    }

                                    // Move count / Castling
                                    Text(
                                        "Move #${pos.fullmoveNumber} • Castling: ${pos.castlingRights.toFen()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Mini 2D board preview
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                ) {
                                    ChessBoard2D(
                                        position = pos,
                                        selectedSquare = null,
                                        legalMoves = emptyList(),
                                        onSquareClicked = {},
                                        lastMove = null,
                                        engineArrowMove = null,
                                        orientation = pos.activeColor,
                                        theme = state.boardTheme
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (validateFen(fenInput)) {
                            step = 2
                        }
                    },
                    modifier = Modifier.testTag("fen_validate_button"),
                    enabled = fenInput.isNotBlank() && validationError == null
                ) {
                    Text("Continue")
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    } else {
        // Step 2: Mode & Color Selection
        val position = validatedPosition ?: return
        val fenActiveColor = position.activeColor

        AlertDialog(
            onDismissRequest = { step = 1 },
            title = {
                Text("Select Game Mode", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Position status pill
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                "Position loaded with ${if (fenActiveColor == PieceColor.WHITE) "White (♙)" else "Black (♟)"} to move.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Mode 1: Play Against Bot
                    val isBotSelected = selectedMode == GameMode.PLAYER_VS_AI
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = if (isBotSelected) 2.dp else 1.dp,
                                color = if (isBotSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedMode = GameMode.PLAYER_VS_AI }
                            .testTag("fen_mode_vs_bot"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isBotSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.SmartToy, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("Play Against Bot", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            }
                            Text(
                                "Challenge the engine from this position. Bot automatically responds on its turns.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (isBotSelected) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                Text("Choose your color:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = selectedPlayerColor == PieceColor.WHITE,
                                        onClick = { selectedPlayerColor = PieceColor.WHITE },
                                        label = { Text("White ♙") },
                                        modifier = Modifier.weight(1f).testTag("fen_color_white")
                                    )
                                    FilterChip(
                                        selected = selectedPlayerColor == PieceColor.BLACK,
                                        onClick = { selectedPlayerColor = PieceColor.BLACK },
                                        label = { Text("Black ♟") },
                                        modifier = Modifier.weight(1f).testTag("fen_color_black")
                                    )
                                }

                                val botTurnFirst = selectedPlayerColor != fenActiveColor
                                Text(
                                    text = if (botTurnFirst) {
                                        "⚡ Bot (${if (fenActiveColor == PieceColor.WHITE) "White" else "Black"}) will automatically play the first move."
                                    } else {
                                        "✓ You play as ${if (selectedPlayerColor == PieceColor.WHITE) "White" else "Black"} and make the first move."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (botTurnFirst) MaterialTheme.colorScheme.primary else Color(0xFF10B981),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Mode 2: Bot Helper
                    val isHelperSelected = selectedMode == GameMode.HELPER_BOT
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = if (isHelperSelected) 2.dp else 1.dp,
                                color = if (isHelperSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedMode = GameMode.HELPER_BOT }
                            .testTag("fen_mode_helper"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isHelperSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Assistant, contentDescription = null, tint = Color(0xFFF59E0B))
                                Text("Bot Helper / Friend", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            }
                            Text(
                                "Bot acts as your personal friend & helper for your chosen side. Opponent moves are played manually.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (isHelperSelected) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                Text("Helper assists side:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = selectedHelperColor == PieceColor.WHITE,
                                        onClick = { selectedHelperColor = PieceColor.WHITE },
                                        label = { Text("White ♙") },
                                        modifier = Modifier.weight(1f).testTag("fen_helper_white")
                                    )
                                    FilterChip(
                                        selected = selectedHelperColor == PieceColor.BLACK,
                                        onClick = { selectedHelperColor = PieceColor.BLACK },
                                        label = { Text("Black ♟") },
                                        modifier = Modifier.weight(1f).testTag("fen_helper_black")
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Auto-play Helper moves", style = MaterialTheme.typography.bodySmall)
                                    Switch(
                                        checked = helperAutoPlay,
                                        onCheckedChange = { helperAutoPlay = it },
                                        modifier = Modifier.testTag("fen_helper_autoplay_switch")
                                    )
                                }

                                val helperTurnFirst = selectedHelperColor == fenActiveColor
                                Text(
                                    text = if (helperTurnFirst) {
                                        if (helperAutoPlay) "⚡ Helper (${if (selectedHelperColor == PieceColor.WHITE) "White" else "Black"}) will automatically play the first move."
                                        else "⚡ Helper (${if (selectedHelperColor == PieceColor.WHITE) "White" else "Black"}) will show recommended move arrow for the first turn."
                                    } else {
                                        "✓ Helper is set to ${if (selectedHelperColor == PieceColor.WHITE) "White" else "Black"}. You make the opponent's first move manually on the board."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (helperTurnFirst) MaterialTheme.colorScheme.primary else Color(0xFF10B981),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val playerColor = if (selectedMode == GameMode.HELPER_BOT) selectedHelperColor else selectedPlayerColor
                        val started = viewModel.startCustomGame(
                            fenString = fenInput.trim(),
                            mode = selectedMode,
                            playerColor = playerColor,
                            helperColor = selectedHelperColor,
                            helperAutoPlay = helperAutoPlay
                        )
                        if (started) {
                            onDismiss()
                            onFenLoaded()
                        }
                    },
                    modifier = Modifier.testTag("fen_start_button")
                ) {
                    Text("Start Game")
                }
            },
            dismissButton = {
                TextButton(onClick = { step = 1 }) {
                    Text("Back")
                }
            }
        )
    }
}

/**
 * Backward compatibility overlay composable for test tags and external references.
 */
@Composable
fun FenLoadOverlay(
    modifier: Modifier = Modifier,
    viewModel: ChessViewModel,
    onFenLoaded: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        OutlinedButton(
            onClick = { showDialog = true },
            modifier = Modifier.fillMaxWidth().testTag("fen_load_button")
        ) {
            Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Load FEN String")
        }
    }

    FenLoadDialog(
        isOpen = showDialog,
        onDismiss = { showDialog = false },
        viewModel = viewModel,
        onFenLoaded = onFenLoaded
    )
}
