package com.example.chess.ui.screens

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import com.example.chess.ui.ChessViewModel


@Composable
fun PlayScreen(
    viewModel: ChessViewModel,
    onBackToMenu: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.status) {
        if (state.status != com.example.chess.model.GameStatus.IN_PROGRESS) {
            showDialog = true
        }
    }

    LaunchedEffect(state.position) {
        // Reset dismiss flag when a new game starts (position resets to initial)
        if (state.position == com.example.chess.model.ChessPosition.initial()) {
            showDialog = false
        }
    }

    GameScreen(
        viewModel = viewModel,
        onBackToMenu = onBackToMenu,
        onOpenSettings = onOpenSettings,
        modifier = modifier
    )

    if (showDialog) {
        val text = when (state.status) {
            com.example.chess.model.GameStatus.CHECKMATE -> "Checkmate!"
            com.example.chess.model.GameStatus.STALEMATE -> "Stalemate — Draw"
            com.example.chess.model.GameStatus.DRAW_FIFTY_MOVE -> "Draw — 50-move rule"
            com.example.chess.model.GameStatus.DRAW_INSUFFICIENT_MATERIAL -> "Draw — Insufficient material"
            com.example.chess.model.GameStatus.DRAW_REPETITION -> "Draw — Threefold repetition"
            else -> ""
        }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Game Over") },
            text = { Text(text) },
            confirmButton = {
                Button(onClick = {
                    viewModel.resetGame()
                    showDialog = false
                }) {
                    Text("New Game")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    viewModel.enterAnalysisMode()
                    showDialog = false
                }) {
                    Text("Analysis")
                }
            }
        )
    }
}
