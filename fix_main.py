with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

content = content.replace(
    "ScreenState.MAIN_MENU -> MainMenuScreen(viewModel, onStartGame = { currentScreen = ScreenState.PLAY_BOARD }, onChangeEngine = { currentScreen = ScreenState.ENGINE_DISCOVERY }, onOpenPuzzles = { currentScreen = ScreenState.PUZZLES }, onOpenHistory = { currentScreen = ScreenState.HISTORY }, onOpenSettings = { currentScreen = ScreenState.SETTINGS })",
    "ScreenState.MAIN_MENU -> MainMenuScreen(viewModel, onStartGame = { currentScreen = ScreenState.PLAY_BOARD }, onChangeEngine = { currentScreen = ScreenState.ENGINE_DISCOVERY }, onOpenPuzzles = { currentScreen = ScreenState.PUZZLES }, onOpenHistory = { currentScreen = ScreenState.HISTORY }, onOpenSettings = { currentScreen = ScreenState.SETTINGS }, onOpenVisionPuzzle = { currentScreen = ScreenState.IMAGE_PUZZLE }, onOpenBoardEditor = { currentScreen = ScreenState.CUSTOM_BOARD })"
)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
