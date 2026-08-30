package com.example.chess.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.data.GameRecord
import com.example.chess.model.GameMode
import com.example.chess.model.PieceColor
import com.example.chess.ui.ChessViewModel
import com.example.chess.ui.components.GameModeCardLarge
import com.example.chess.ui.components.RecentGameCard
import com.example.chess.ui.components.SmallModeCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
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
    val state by viewModel.uiState.collectAsState()
    val recentGames by viewModel.allGameHistory.collectAsState(initial = emptyList())

    var showPvBotDialog by remember { mutableStateOf(false) }
    var showHelperBotDialog by remember { mutableStateOf(false) }
    var showFenDialog by remember { mutableStateOf(false) }
    var selectedPlayerColor by remember { mutableStateOf(PieceColor.WHITE) }
    var selectedHelperColor by remember { mutableStateOf(PieceColor.WHITE) }
    var helperAutoPlay by remember { mutableStateOf(true) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F141C),
                        Color(0xFF161E2B),
                        Color(0xFF1D2736)
                    )
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            contentPadding = PaddingValues(top = 28.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. App Hero Branding Banner
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Animated Chess Crown Badge
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFFFFD700), Color(0xFFC79A00))
                                ),
                                shape = CircleShape
                            )
                            .shadow(16.dp, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "♔",
                            fontSize = 46.sp,
                            color = Color(0xFF0F141C),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Chess Master Pro",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF10B981), CircleShape)
                        )
                        Text(
                            text = "Stockfish 18 • Max Power Master",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }

            // 2. Engine Status Bar (Quick Config)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChangeEngine() }
                        .testTag("menu_engine_banner"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161E2B)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF10B981).copy(alpha = 0.20f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.activeEngineName,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "Skill Level 20 • 256MB Hash • 3200 ELO",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF64748B)
                                )
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Config engine",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // 3. Play Modes Section
            item {
                Text(
                    text = "Play Modes",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            // Play vs Bot Card
            item {
                GameModeCardLarge(
                    title = "Play vs Bot",
                    subtitle = "Challenge Stockfish 18",
                    description = "Battle against the grandmaster engine at full master strength",
                    icon = "🤖",
                    gradientColors = listOf(Color(0xFF10B981), Color(0xFF047857)),
                    onClick = { showPvBotDialog = true }
                )
            }

            // Helper Bot Card
            item {
                GameModeCardLarge(
                    title = "Helper Bot",
                    subtitle = "Learn with AI Coach",
                    description = "Stockfish calculates and suggests the best moves continuously",
                    icon = "💡",
                    gradientColors = listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)),
                    onClick = { showHelperBotDialog = true }
                )
            }

            // Pass & Play Card
            item {
                GameModeCardLarge(
                    title = "Pass & Play",
                    subtitle = "Two Players",
                    description = "Over-the-board 2-player match on the same screen",
                    icon = "👥",
                    gradientColors = listOf(Color(0xFFF59E0B), Color(0xFFB45309)),
                    onClick = {
                        viewModel.startPassAndPlay()
                        onStartGame()
                    }
                )
            }

            // 4. Practice & Tools Section
            item {
                Text(
                    text = "Practice & Analysis",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SmallModeCard(
                        title = "Analysis",
                        icon = "📊",
                        color = Color(0xFFA855F7),
                        onClick = {
                            viewModel.startAnalysis()
                            onStartGame()
                        },
                        modifier = Modifier.weight(1f)
                    )

                    SmallModeCard(
                        title = "Puzzles",
                        icon = "🧩",
                        color = Color(0xFFEC4899),
                        onClick = onOpenPuzzles,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SmallModeCard(
                        title = "Setup Board",
                        icon = "⚙️",
                        color = Color(0xFF06B6D4),
                        onClick = onOpenBoardEditor,
                        modifier = Modifier.weight(1f)
                    )

                    SmallModeCard(
                        title = "AI Vision",
                        icon = "📷",
                        color = Color(0xFFEAB308),
                        onClick = onOpenVisionPuzzle,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 5. Recent Games
            if (recentGames.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent Matches",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }

                items(recentGames.take(3).size) { index ->
                    val game = recentGames[index]
                    RecentGameCard(
                        game = game,
                        onClick = {
                            viewModel.loadFen(game.finalFen)
                            onStartGame()
                        }
                    )
                }
            }

            // 6. Bottom Navigation Tools
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = onOpenHistory) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Match History",
                            tint = Color(0xFFCBD5E1)
                        )
                    }

                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "App Settings",
                            tint = Color(0xFFCBD5E1)
                        )
                    }

                    IconButton(onClick = onChangeEngine) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = "Engine Discovery",
                            tint = Color(0xFFCBD5E1)
                        )
                    }
                }
            }
        }
    }

    // Play vs Bot Dialog
    if (showPvBotDialog) {
        AlertDialog(
            onDismissRequest = { showPvBotDialog = false },
            title = { Text("Play vs Stockfish 18", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Select your side:", color = Color(0xFF94A3B8), fontSize = 14.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { selectedPlayerColor = PieceColor.WHITE },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedPlayerColor == PieceColor.WHITE) Color(0xFF10B981) else Color(0xFF1E293B)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("White ♔", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { selectedPlayerColor = PieceColor.BLACK },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedPlayerColor == PieceColor.BLACK) Color(0xFF10B981) else Color(0xFF1E293B)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Black ♚", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPvBotDialog = false
                        viewModel.startPlayerVsBot(selectedPlayerColor)
                        onStartGame()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Start Match", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPvBotDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF161E2B)
        )
    }

    // Helper Bot Dialog
    if (showHelperBotDialog) {
        AlertDialog(
            onDismissRequest = { showHelperBotDialog = false },
            title = { Text("Helper Bot AI", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Which side should the AI assist?", color = Color(0xFF94A3B8), fontSize = 14.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { selectedHelperColor = PieceColor.WHITE },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedHelperColor == PieceColor.WHITE) Color(0xFF3B82F6) else Color(0xFF1E293B)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("White ♔", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { selectedHelperColor = PieceColor.BLACK },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedHelperColor == PieceColor.BLACK) Color(0xFF3B82F6) else Color(0xFF1E293B)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Black ♚", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { helperAutoPlay = !helperAutoPlay }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = helperAutoPlay,
                            onCheckedChange = { helperAutoPlay = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF3B82F6))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Auto-play helper moves", color = Color.White, fontSize = 14.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showHelperBotDialog = false
                        viewModel.startHelperBot(selectedHelperColor, helperAutoPlay)
                        onStartGame()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                ) {
                    Text("Start AI Helper", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showHelperBotDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF161E2B)
        )
    }
}
