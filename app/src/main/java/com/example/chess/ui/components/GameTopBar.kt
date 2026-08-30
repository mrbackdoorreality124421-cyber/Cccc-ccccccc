package com.example.chess.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameTopBar(
    whitePlayer: String,
    blackPlayer: String,
    evaluation: Int,
    mateIn: Int?,
    depth: Int,
    isThinking: Boolean,
    isAssistantActive: Boolean,
    isSoundActive: Boolean,
    onBack: () -> Unit,
    onToggleAssistant: () -> Unit,
    onToggleSound: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking_anim")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin_gear"
    )

    TopAppBar(
        title = {
            Column(verticalArrangement = Arrangement.Center) {
                // Players header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = whitePlayer,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF8FAFC)
                    )
                    Text(
                        text = "vs",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = blackPlayer,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                }

                // Subtitle with real-time Eval and Engine Depth
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val evalText = if (mateIn != null) {
                        "M${abs(mateIn)}"
                    } else {
                        val v = evaluation / 100.0
                        if (v > 0) "+%.2f".format(v) else "%.2f".format(v)
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                color = if (evaluation >= 0) Color(0xFF10B981).copy(alpha = 0.25f) else Color(0xFFEF4444).copy(alpha = 0.25f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "Eval: $evalText",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (evaluation >= 0) Color(0xFF34D399) else Color(0xFFF87171)
                        )
                    }

                    if (depth > 0) {
                        Text(
                            text = "D:$depth",
                            fontSize = 10.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    if (isThinking) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = "Engine thinking",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier
                                    .size(12.dp)
                                    .rotate(rotation)
                            )
                            Text(
                                text = "Thinking...",
                                fontSize = 10.sp,
                                color = Color(0xFFFFD700),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Menu",
                    tint = Color.White
                )
            }
        },
        actions = {
            // Assistant Toggle Pill
            IconButton(
                onClick = onToggleAssistant,
                modifier = Modifier
                    .background(
                        if (isAssistantActive) Color(0xFF3B82F6).copy(alpha = 0.25f) else Color.Transparent,
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = "Toggle Assistant",
                    tint = if (isAssistantActive) Color(0xFFFFD700) else Color(0xFF64748B)
                )
            }

            // Sound Toggle
            IconButton(onClick = onToggleSound) {
                Icon(
                    imageVector = if (isSoundActive) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                    contentDescription = "Toggle Sound",
                    tint = if (isSoundActive) Color(0xFF34D399) else Color(0xFF64748B)
                )
            }

            // Settings
            IconButton(onClick = onSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Game Settings",
                    tint = Color(0xFFCBD5E1)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF0F141C)
        ),
        modifier = modifier
    )
}
