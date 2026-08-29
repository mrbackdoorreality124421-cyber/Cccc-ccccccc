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
    var selectedMode by remember { mutableStateOf(GameMode.PLAYER_VS_AI) }
    var selectedColor by remember { mutableStateOf(PieceColor.WHITE) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        OutlinedButton(onClick = { error = null; showFenDialog = true }) {
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
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("FEN") },
                        supportingText = { Text(error ?: "Paste a complete chess FEN position.") },
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (ChessPosition.fromFen(fen) != null) {
                        showFenDialog = false
                        showModeDialog = true
                    } else {
                        error = "Invalid FEN. Please check the position and all FEN fields."
                    }
                }) { Text("Validate & Continue") }
            },
            dismissButton = { TextButton(onClick = { showFenDialog = false }) { Text("Cancel") } }
        )
    }

    if (showModeDialog) {
        AlertDialog(
            onDismissRequest = { showModeDialog = false },
            title = { Text("FEN Position Mode") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Choose how you want to use this exact FEN position:")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedMode == GameMode.PLAYER_VS_AI,
                            onClick = { selectedMode = GameMode.PLAYER_VS_AI },
                            label = { Text("Play Against Bot") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedMode == GameMode.HELPER_BOT,
                            onClick = { selectedMode = GameMode.HELPER_BOT },
                            label = { Text("Bot Helper") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (selectedMode == GameMode.PLAYER_VS_AI) {
                        Text("Choose your side:")
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = selectedColor == PieceColor.WHITE,
                                onClick = { selectedColor = PieceColor.WHITE },
                                label = { Text("White") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedColor == PieceColor.BLACK,
                                onClick = { selectedColor = PieceColor.BLACK },
                                label = { Text("Black") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        Text("Helper mode keeps the loaded side-to-move and gives you recommendations without automatically playing the move.")
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                }
            },
            confirmButton = {
                Button(onClick = {
                    val started = viewModel.startCustomGame(
                        fenString = fen,
                        mode = selectedMode,
                        playerColor = selectedColor
                    )
                    if (started) {
                        if (selectedMode == GameMode.HELPER_BOT) {
                            viewModel.toggleHelperAutoPlay()
                        }
                        showModeDialog = false
                        onFenLoaded()
                    }
                }) { Text("Start") }
            },
            dismissButton = { TextButton(onClick = { showModeDialog = false }) { Text("Back") } }
        )
    }
}
