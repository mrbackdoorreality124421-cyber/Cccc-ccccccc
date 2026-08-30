package com.example.chess.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.chess.model.ChessMove

@Composable
fun MoveHistoryView(
    moves: List<ChessMove>,
    modifier: Modifier = Modifier
) {
    MoveListCompact(moves = moves, modifier = modifier)
}
