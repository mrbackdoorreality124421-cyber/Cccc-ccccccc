package com.example.chess.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.model.BoardTheme

@Composable
fun BoardCoordinates(
    isFlipped: Boolean,
    theme: BoardTheme,
    modifier: Modifier = Modifier
) {
    val textColor = Color(theme.darkColor).copy(alpha = 0.85f)

    Box(modifier = modifier.fillMaxSize()) {
        // Files (a - h) along the bottom edge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 2.dp, start = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            for (file in 0..7) {
                val displayFile = if (isFlipped) 7 - file else file
                val fileChar = ('a'.code + displayFile).toChar().toString()
                Text(
                    text = fileChar,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
        }

        // Ranks (1 - 8) along the left edge
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .align(Alignment.CenterStart)
                .padding(start = 3.dp, top = 4.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.SpaceAround
        ) {
            for (rank in 7 downTo 0) {
                val displayRank = if (isFlipped) 7 - rank else rank
                val rankNum = (displayRank + 1).toString()
                Text(
                    text = rankNum,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
        }
    }
}
