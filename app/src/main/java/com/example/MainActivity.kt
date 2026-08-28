package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chess.ui.ChessViewModel
import com.example.chess.ui.screens.CustomBoardScreen
import com.example.chess.ui.screens.EngineDiscoveryHomeScreen
import com.example.chess.ui.screens.HistoryScreen
import com.example.chess.ui.screens.MainMenuScreen
import com.example.chess.ui.screens.PlayScreen
import com.example.chess.ui.screens.PuzzlesScreen
import com.example.chess.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme

enum class ScreenState {
    ENGINE_DISCOVERY, MAIN_MENU, PLAY_BOARD, PUZZLES, HISTORY, SETTINGS, CUSTOM_BOARD
}

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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
        topBar = {
            if (currentScreen != ScreenState.ENGINE_DISCOVERY && currentScreen != ScreenState.PLAY_BOARD && currentScreen != ScreenState.CUSTOM_BOARD) {
                TopAppBar(
                    title = {
                        Text(
                            when (currentScreen) {
                                ScreenState.ENGINE_DISCOVERY -> "Engine Discovery"
                                ScreenState.MAIN_MENU -> "Chess Engine Hub"
                                ScreenState.PLAY_BOARD -> "Live Chess Match"
                                ScreenState.PUZZLES -> "Tactical Puzzles"
                                ScreenState.HISTORY -> "Match History"
                                ScreenState.SETTINGS -> "Engine & Board Settings"
                                ScreenState.CUSTOM_BOARD -> "Custom Board"
                            },
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    actions = {
                        IconButton(onClick = { currentScreen = ScreenState.CUSTOM_BOARD }, modifier = Modifier.testTag("nav_btn_custom_board")) {
                            Icon(imageVector = Icons.Default.Build, contentDescription = "Custom Board")
                        }
                        IconButton(onClick = { currentScreen = ScreenState.ENGINE_DISCOVERY }, modifier = Modifier.testTag("nav_btn_engine_scan")) {
                            Icon(imageVector = Icons.Default.Memory, contentDescription = "Engines")
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (currentScreen != ScreenState.ENGINE_DISCOVERY && currentScreen != ScreenState.PLAY_BOARD && currentScreen != ScreenState.CUSTOM_BOARD) {
                NavigationBar(tonalElevation = 6.dp) {
                    NavigationBarItem(
                        selected = currentScreen == ScreenState.MAIN_MENU,
                        onClick = { currentScreen = ScreenState.MAIN_MENU },
                        icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Main Menu") },
                        label = { Text("Menu") },
                        modifier = Modifier.testTag("nav_tab_menu")
                    )
                    NavigationBarItem(
                        selected = currentScreen == ScreenState.PLAY_BOARD,
                        onClick = { currentScreen = ScreenState.PLAY_BOARD },
                        icon = { Icon(imageVector = Icons.Default.SportsEsports, contentDescription = "Board") },
                        label = { Text("Board") },
                        modifier = Modifier.testTag("nav_tab_board")
                    )
                    NavigationBarItem(
                        selected = currentScreen == ScreenState.PUZZLES,
                        onClick = { currentScreen = ScreenState.PUZZLES },
                        icon = { Icon(imageVector = Icons.Default.Extension, contentDescription = "Puzzles") },
                        label = { Text("Puzzles") },
                        modifier = Modifier.testTag("nav_tab_puzzles")
                    )
                    NavigationBarItem(
                        selected = currentScreen == ScreenState.HISTORY,
                        onClick = { currentScreen = ScreenState.HISTORY },
                        icon = { Icon(imageVector = Icons.Default.History, contentDescription = "History") },
                        label = { Text("History") },
                        modifier = Modifier.testTag("nav_tab_history")
                    )
                    NavigationBarItem(
                        selected = currentScreen == ScreenState.SETTINGS,
                        onClick = { currentScreen = ScreenState.SETTINGS },
                        icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                        modifier = Modifier.testTag("nav_tab_settings")
                    )
                }
            }
        }
    ) { innerPadding ->
        Crossfade(targetState = currentScreen, label = "screen_crossfade", modifier = Modifier.padding(innerPadding)) { screen ->
            when (screen) {
                ScreenState.ENGINE_DISCOVERY -> EngineDiscoveryHomeScreen(viewModel, onEngineSelected = { currentScreen = ScreenState.MAIN_MENU })
                ScreenState.MAIN_MENU -> MainMenuScreen(
                    viewModel = viewModel,
                    onStartGame = { currentScreen = ScreenState.PLAY_BOARD },
                    onChangeEngine = { currentScreen = ScreenState.ENGINE_DISCOVERY },
                    onOpenPuzzles = { currentScreen = ScreenState.PUZZLES },
                    onOpenHistory = { currentScreen = ScreenState.HISTORY },
                    onOpenSettings = { currentScreen = ScreenState.SETTINGS }
                )
                ScreenState.PLAY_BOARD -> PlayScreen(viewModel, onBackToMenu = { currentScreen = ScreenState.MAIN_MENU }, onOpenSettings = { currentScreen = ScreenState.SETTINGS })
                ScreenState.PUZZLES -> PuzzlesScreen(viewModel, onPuzzleSelected = { currentScreen = ScreenState.PLAY_BOARD })
                ScreenState.HISTORY -> HistoryScreen(viewModel)
                ScreenState.SETTINGS -> SettingsScreen(viewModel, onFenLoaded = { currentScreen = ScreenState.PLAY_BOARD })
                ScreenState.CUSTOM_BOARD -> CustomBoardScreen(viewModel, onBack = { currentScreen = ScreenState.MAIN_MENU }, onAnalyze = { currentScreen = ScreenState.PLAY_BOARD })
            }
        }
    }
}
