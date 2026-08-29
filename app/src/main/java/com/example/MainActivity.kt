package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.ui.ChessViewModel
import com.example.chess.ui.screens.*
import com.example.ui.theme.ChessEmerald
import com.example.ui.theme.ChessGold
import com.example.ui.theme.MyApplicationTheme

enum class ScreenState { ENGINE_DISCOVERY, MAIN_MENU, PLAY_BOARD, PUZZLES, HISTORY, SETTINGS, CUSTOM_BOARD, IMAGE_PUZZLE }

class MainActivity : ComponentActivity() {
    private val viewModel: ChessViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MyApplicationTheme { ChessAppRoot(viewModel) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChessAppRoot(viewModel: ChessViewModel) {
    var currentScreen by remember { mutableStateOf(ScreenState.ENGINE_DISCOVERY) }
    val secondary = currentScreen != ScreenState.ENGINE_DISCOVERY && currentScreen != ScreenState.MAIN_MENU
    val ownChrome = currentScreen == ScreenState.PLAY_BOARD || currentScreen == ScreenState.CUSTOM_BOARD || currentScreen == ScreenState.IMAGE_PUZZLE || currentScreen == ScreenState.MAIN_MENU

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (currentScreen == ScreenState.PUZZLES || currentScreen == ScreenState.HISTORY || currentScreen == ScreenState.SETTINGS) {
                TopAppBar(
                    title = {
                        if (currentScreen == ScreenState.MAIN_MENU) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                BrandMark()
                                Column {
                                    Text("CHESS FORGE", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.1.sp, style = MaterialTheme.typography.titleMedium)
                                    Text("ENGINE • PUZZLES • ANALYSIS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else {
                            Text(screenTitle(currentScreen), fontWeight = FontWeight.Bold)
                        }
                    },
                    navigationIcon = {
                        if (secondary) {
                            IconButton(onClick = { currentScreen = ScreenState.MAIN_MENU }, modifier = Modifier.testTag("global_back")) {
                                Icon(Icons.Default.ArrowBack, "Back")
                            }
                        }
                    },
                    actions = {
                        if (currentScreen == ScreenState.MAIN_MENU) {
                            IconButton(onClick = { currentScreen = ScreenState.IMAGE_PUZZLE }, modifier = Modifier.testTag("nav_btn_image_puzzle")) { Icon(Icons.Default.AutoAwesome, "Image Puzzle") }
                            IconButton(onClick = { currentScreen = ScreenState.CUSTOM_BOARD }, modifier = Modifier.testTag("nav_btn_custom_board")) { Icon(Icons.Default.GridOn, "Custom Board") }
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (currentScreen == ScreenState.MAIN_MENU || currentScreen == ScreenState.PUZZLES || currentScreen == ScreenState.HISTORY || currentScreen == ScreenState.SETTINGS) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                    NavigationBarItem(currentScreen == ScreenState.MAIN_MENU, { currentScreen = ScreenState.MAIN_MENU }, { Icon(Icons.Default.Home, null) }, label = { Text("Home") }, modifier = Modifier.testTag("nav_tab_menu"))
                    NavigationBarItem(currentScreen == ScreenState.PLAY_BOARD, { currentScreen = ScreenState.PLAY_BOARD }, { Icon(Icons.Default.SportsEsports, null) }, label = { Text("Play") }, modifier = Modifier.testTag("nav_tab_board"))
                    NavigationBarItem(currentScreen == ScreenState.PUZZLES, { currentScreen = ScreenState.PUZZLES }, { Icon(Icons.Default.Extension, null) }, label = { Text("Puzzles") }, modifier = Modifier.testTag("nav_tab_puzzles"))
                    NavigationBarItem(currentScreen == ScreenState.HISTORY, { currentScreen = ScreenState.HISTORY }, { Icon(Icons.Default.History, null) }, label = { Text("History") }, modifier = Modifier.testTag("nav_tab_history"))
                    NavigationBarItem(currentScreen == ScreenState.SETTINGS, { currentScreen = ScreenState.SETTINGS }, { Icon(Icons.Default.Settings, null) }, label = { Text("Settings") }, modifier = Modifier.testTag("nav_tab_settings"))
                }
            }
        }
    ) { innerPadding ->
        Crossfade(targetState = currentScreen, label = "screen_transition", modifier = Modifier.padding(innerPadding)) { screen ->
            when (screen) {
                ScreenState.ENGINE_DISCOVERY -> EngineDiscoveryHomeScreen(viewModel, onEngineSelected = { currentScreen = ScreenState.MAIN_MENU })
                ScreenState.MAIN_MENU -> MainMenuScreen(viewModel, onStartGame = { currentScreen = ScreenState.PLAY_BOARD }, onChangeEngine = { currentScreen = ScreenState.ENGINE_DISCOVERY }, onOpenPuzzles = { currentScreen = ScreenState.PUZZLES }, onOpenHistory = { currentScreen = ScreenState.HISTORY }, onOpenSettings = { currentScreen = ScreenState.SETTINGS }, onOpenVisionPuzzle = { currentScreen = ScreenState.IMAGE_PUZZLE }, onOpenBoardEditor = { currentScreen = ScreenState.CUSTOM_BOARD })
                ScreenState.PLAY_BOARD -> PlayScreen(viewModel, onBackToMenu = { currentScreen = ScreenState.MAIN_MENU }, onOpenSettings = { currentScreen = ScreenState.SETTINGS })
                ScreenState.PUZZLES -> PuzzlesScreen(viewModel, onPuzzleSelected = { currentScreen = ScreenState.PLAY_BOARD })
                ScreenState.HISTORY -> HistoryScreen(viewModel)
                ScreenState.SETTINGS -> SettingsScreen(viewModel, onFenLoaded = { currentScreen = ScreenState.PLAY_BOARD })
                ScreenState.CUSTOM_BOARD -> CustomBoardScreen(viewModel, onBack = { currentScreen = ScreenState.MAIN_MENU }, onAnalyze = { currentScreen = ScreenState.PLAY_BOARD })
                ScreenState.IMAGE_PUZZLE -> ImagePuzzleScreen(viewModel, onBack = { currentScreen = ScreenState.MAIN_MENU }, onOpenCustomBoard = { currentScreen = ScreenState.CUSTOM_BOARD }, onAnalyze = { currentScreen = ScreenState.PLAY_BOARD })
            }
        }
    }
}

@Composable
private fun BrandMark() {
    Box(
        modifier = Modifier.size(42.dp).clip(CircleShape).background(ChessEmerald),
        contentAlignment = Alignment.Center
    ) {
        Text("♞", color = ChessGold, fontSize = 25.sp, fontWeight = FontWeight.Bold)
    }
}

private fun screenTitle(screen: ScreenState): String = when (screen) {
    ScreenState.PUZZLES -> "Tactical Puzzles"
    ScreenState.HISTORY -> "Match History"
    ScreenState.SETTINGS -> "Settings"
    ScreenState.PLAY_BOARD -> "Play"
    ScreenState.CUSTOM_BOARD -> "Custom Board"
    ScreenState.IMAGE_PUZZLE -> "Image Puzzle"
    else -> "Chess Forge"
}
