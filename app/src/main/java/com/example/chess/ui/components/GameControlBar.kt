package com.example.chess.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GameControlBar(
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onFlip: () -> Unit,
    onNewGame: () -> Unit,
    onHint: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF161E2B), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Undo Button
        IconButton(
            onClick = onUndo,
            enabled = canUndo
        ) {
            Icon(
                imageVector = Icons.Default.Undo,
                contentDescription = "Undo Move",
                tint = if (canUndo) Color(0xFFF8FAFC) else Color(0xFF475569)
            )
        }

        // Redo Button
        IconButton(
            onClick = onRedo,
            enabled = canRedo
        ) {
            Icon(
                imageVector = Icons.Default.Redo,
                contentDescription = "Redo Move",
                tint = if (canRedo) Color(0xFFF8FAFC) else Color(0xFF475569)
            )
        }

        // Hint Button (AI suggestion)
        FilledTonalIconButton(
            onClick = onHint,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = Color(0xFF1E293B),
                contentColor = Color(0xFFFFD700)
            )
        ) {
            Icon(
                imageVector = Icons.Default.AutoFixHigh,
                contentDescription = "AI Best Move Hint"
            )
        }

        // Flip Board Button
        IconButton(onClick = onFlip) {
            Icon(
                imageVector = Icons.Default.Sync,
                contentDescription = "Flip Board Orientation",
                tint = Color(0xFFF8FAFC)
            )
        }

        // Restart / New Game Button
        IconButton(onClick = onNewGame) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Restart Game",
                tint = Color(0xFFF8FAFC)
            )
        }
    }
}
