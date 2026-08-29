with open("app/src/main/java/com/example/chess/ui/screens/MainMenuScreen.kt", "r") as f:
    content = f.read()

header = """
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF10B981)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("♞", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("CHESS FORGE", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp, style = MaterialTheme.typography.titleLarge)
                            Text("ENGINE • PUZZLES • ANALYSIS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
"""

content = content.replace("            // Header\n", header + "            // Header\n")

with open("app/src/main/java/com/example/chess/ui/screens/MainMenuScreen.kt", "w") as f:
    f.write(content)
