package com.example.chess.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.chess.ui.ChessViewModel

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
    HomeScreen(
        viewModel = viewModel,
        onStartGame = onStartGame,
        onChangeEngine = onChangeEngine,
        onOpenPuzzles = onOpenPuzzles,
        onOpenHistory = onOpenHistory,
        onOpenSettings = onOpenSettings,
        onOpenVisionPuzzle = onOpenVisionPuzzle,
        onOpenBoardEditor = onOpenBoardEditor,
        modifier = modifier
    )
}
