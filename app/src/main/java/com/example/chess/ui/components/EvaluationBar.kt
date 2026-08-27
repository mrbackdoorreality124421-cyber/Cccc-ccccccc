package com.example.chess.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

@Composable
fun EvaluationBar(
    evalCp: Int,
    mateIn: Int?,
    isThinking: Boolean,
    modifier: Modifier = Modifier
) {
    // Evaluation mapped between 0.0 (Black winning) and 1.0 (White winning)
    // 0 cp -> 0.5 ratio
    val rawRatio = when {
        mateIn != null -> if (mateIn > 0) 0.98f else 0.02f
        else -> {
            val clampedCp = evalCp.coerceIn(-1000, 1000)
            0.5f + (clampedCp / 2000f)
        }
    }

    val animatedRatio by animateFloatAsState(targetValue = rawRatio, label = "eval_ratio")

    val evalText = when {
        mateIn != null -> "M${abs(mateIn)}"
        else -> {
            val score = evalCp / 100.0
            if (evalCp > 0) "+%.1f".format(score) else "%.1f".format(score)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E293B)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // White share (left)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(animatedRatio.coerceIn(0.05f, 0.95f))
                .background(Color(0xFFF1F5F9))
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (animatedRatio >= 0.5f) {
                Text(
                    text = evalText,
                    color = Color(0xFF0F172A),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Black share (right)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight((1f - animatedRatio).coerceIn(0.05f, 0.95f))
                .background(Color(0xFF0F172A))
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            if (animatedRatio < 0.5f) {
                Text(
                    text = evalText,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
