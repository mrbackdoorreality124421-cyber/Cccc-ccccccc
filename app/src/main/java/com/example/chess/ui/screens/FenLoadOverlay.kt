package com.example.chess.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.chess.model.ChessPosition
import com.example.chess.model.GameMode
import com.example.chess.model.PieceColor
import com.example.chess.ui.ChessViewModel

@Composable
fun FenLoadOverlay(
    viewModel: ChessViewModel,
    onFenLoaded: () -> Unit,
    modifier: Modifier = Modifier
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

    if (showFenDialog) {
        AlertDialog(
            onDismissRequest = { showFenDialog = false },
            title = { Text("Load FEN String") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = fen,
                        onValueChange = { fen = it; error = null },
                        modifier = Modifier.fillMaxWidth().testTag("fen_input"),
                        label = { Text("FEN") },
                        supportingText = { Text(error ?: "Paste the complete FEN position (all 6 fields).") },
                        minLines = 3,
                        singleLine = false
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val position = ChessPosition.fromFen(fen)
                        if (position != null && fen.trim().split(Regex("\\s+")).size == 6) {
                            validatedPosition = position
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
            dismissButton = { TextButton(onClick = { showFenDialog = false }) { Text("Cancel") } }
        )
    }

    if (showModeDialog && validatedPosition != null) {
        val position = validatedPosition!!
        AlertDialog(
            onDismissRequest = { showModeDialog = false },
            title = { Text("FEN Position Loaded") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Exact position loaded. Side to move: ${if (position.activeColor == PieceColor.WHITE) "White" else "Black"}."
                    )
                    Text("Choose one of the two FEN modes:")
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

                    if (selectedMode == GameMode.PLAYER_VS_AI) {
                        Text("Choose your color:")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedColor == PieceColor.WHITE,
                                onClick = { selectedColor = PieceColor.WHITE },
                                label = { Text("White") },
                                modifier = Modifier.weight(1f).testTag("fen_color_white")
                            )
                            FilterChip(
                                selected = selectedColor == PieceColor.BLACK,
                                onClick = { selectedColor = PieceColor.BLACK },
                                label = { Text("Black") },
                                modifier = Modifier.weight(1f).testTag("fen_color_black")
                            )
                        }
                        Text(
                            "The FEN side-to-move is preserved. If it is the bot's turn, the bot will move automatically."
                        )
                    } else {
                        Text(
                            "Bot Helper recommends the best move for the current side-to-move. It will show the recommendation arrow but will not play the move automatically."
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
                            playerColor = selectedColor
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
