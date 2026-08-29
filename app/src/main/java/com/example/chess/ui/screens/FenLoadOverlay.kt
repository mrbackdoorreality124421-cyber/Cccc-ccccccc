package com.example.chess.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.chess.model.ChessPosition
import com.example.chess.model.GameMode
import com.example.chess.model.PieceColor
import com.example.chess.viewmodel.ChessViewModel

@Composable
fun FenLoadOverlay(
    modifier: Modifier = Modifier,
    viewModel: ChessViewModel,
    onFenLoaded: () -> Unit
) {
    var showFenDialog by remember { mutableStateOf(false) }
    var showModeDialog by remember { mutableStateOf(false) }
    var fen by remember { mutableStateOf("") }
    var validatedPosition by remember { mutableStateOf<ChessPosition?>(null) }
    var selectedMode by remember { mutableStateOf(GameMode.PLAYER_VS_AI) }
    var selectedColor by remember { mutableStateOf(PieceColor.WHITE) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        OutlinedButton(
            onClick = {
                error = null
                validatedPosition = null
                showFenDialog = true
            },
            modifier = Modifier.testTag("fen_load_button")
        ) {
            Text("Load FEN String")
        }
    }

    // ─── Step 1: FEN Input Dialog ───────────────────────────────────────────
    if (showFenDialog) {
        AlertDialog(
            onDismissRequest = { showFenDialog = false },
            title = { Text("Load FEN Position") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = fen,
                        onValueChange = { fen = it; error = null },
                        modifier = Modifier.fillMaxWidth().testTag("fen_input"),
                        label = { Text("FEN") },
                        supportingText = {
                            Text(error ?: "Paste the complete FEN position (all 6 fields).")
                        },
                        minLines = 3,
                        singleLine = false,
                        isError = error != null
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val position = ChessPosition.fromFen(fen)
                        if (position != null && fen.trim().split(Regex("\\s+")).size == 6) {
                            validatedPosition = position
                            // Auto-set color to match FEN side-to-move as default
                            selectedColor = position.activeColor
                            error = null
                            showFenDialog = false
                            showModeDialog = true
                        } else {
                            error = "Invalid FEN. Enter a complete valid 6-field FEN position."
                        }
                    },
                    modifier = Modifier.testTag("fen_validate_button")
                ) { Text("Validate & Continue") }
            },
            dismissButton = {
                TextButton(onClick = { showFenDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ─── Step 2: Mode + Color Selection Dialog ──────────────────────────────
    if (showModeDialog && validatedPosition != null) {
        val position = validatedPosition!!
        val fenSideToMove = position.activeColor

        AlertDialog(
            onDismissRequest = { showModeDialog = false },
            title = { Text("FEN Position Loaded") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    // Show side-to-move info
                    Text(
                        "Position loaded. Side to move: ${if (fenSideToMove == PieceColor.WHITE) "White ♙" else "Black ♟"}."
                    )

                    Text("Choose one of the two FEN modes:")

                    // Mode Selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedMode == GameMode.PLAYER_VS_AI,
                            onClick = { selectedMode = GameMode.PLAYER_VS_AI },
                            label = { Text("Play Against Bot") },
                            modifier = Modifier.weight(1f).testTag("fen_mode_vs_bot")
                        )
                        FilterChip(
                            selected = selectedMode == GameMode.HELPER_BOT,
                            onClick = { selectedMode = GameMode.HELPER_BOT },
                            label = { Text("Bot Helper") },
                            modifier = Modifier.weight(1f).testTag("fen_mode_helper")
                        )
                    }

                    // Color Selection (only for Play Against Bot)
                    if (selectedMode == GameMode.PLAYER_VS_AI) {
                        Text("Choose your color:")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedColor == PieceColor.WHITE,
                                onClick = { selectedColor = PieceColor.WHITE },
                                label = { Text("White ♙") },
                                modifier = Modifier.weight(1f).testTag("fen_color_white")
                            )
                            FilterChip(
                                selected = selectedColor == PieceColor.BLACK,
                                onClick = { selectedColor = PieceColor.BLACK },
                                label = { Text("Black ♟") },
                                modifier = Modifier.weight(1f).testTag("fen_color_black")
                            )
                        }
                        // Hint about bot first move
                        if (selectedColor != fenSideToMove) {
                            Text(
                                "Bot will play ${if (fenSideToMove == PieceColor.WHITE) "White" else "Black"} first automatically.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        // Helper mode description
                        Text(
                            "Bot Helper recommends the best move for the current side-to-move. " +
                            "It will show the recommendation arrow but will not play the move automatically.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val started = viewModel.startCustomGame(
                            fenString = fen.trim(),
                            mode = selectedMode,
                            playerColor = if (selectedMode == GameMode.HELPER_BOT) fenSideToMove else selectedColor
                        )
                        if (started) {
                            showModeDialog = false
                            validatedPosition = null
                            onFenLoaded()
                        }
                    },
                    modifier = Modifier.testTag("fen_start_button")
                ) { Text("Start") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showModeDialog = false
                        showFenDialog = true
                    }
                ) { Text("Back") }
            }
        )
    }
}