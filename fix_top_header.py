import re

with open("app/src/main/java/com/example/chess/ui/screens/PlayScreen.kt", "r") as f:
    content = f.read()

replacement = """
        // Left Branding & Engine Name
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onBackToMenu,
                modifier = Modifier.size(32.dp).testTag("btn_header_back")
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
"""
content = re.sub(r'\s*// Left Branding & Engine Name\s*Row\(\s*verticalAlignment = Alignment.CenterVertically,\s*horizontalArrangement = Arrangement.spacedBy\(8.dp\)\s*\) \{', replacement, content)

with open("app/src/main/java/com/example/chess/ui/screens/PlayScreen.kt", "w") as f:
    f.write(content)
