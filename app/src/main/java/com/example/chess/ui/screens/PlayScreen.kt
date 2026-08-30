package com.example.chess.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.chess.ui.ChessViewModel

@Composable
fun PlayScreen(
    viewModel: ChessViewModel,
    onBackToMenu: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    GameScreen(
        viewModel = viewModel,
        onBackToMenu = onBackToMenu,
        onOpenSettings = onOpenSettings,
        modifier = modifier
    )
}
