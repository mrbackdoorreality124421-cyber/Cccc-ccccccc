import re

with open("app/src/main/java/com/example/chess/ui/screens/SettingsScreen.kt", "r") as f:
    content = f.read()

content = re.sub(r'\s*// Section 4: Position Setup.*?// FEN Dialog.*?showFenDialog = false \}\n                \}\n            \}\n        \)\n    \}', '', content, flags=re.DOTALL)

with open("app/src/main/java/com/example/chess/ui/screens/SettingsScreen.kt", "w") as f:
    f.write(content)
