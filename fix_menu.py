content = """            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenPuzzles() }
                            .testTag("menu_puzzles"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Extension,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tactical Puzzles",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenHistory() }
                            .testTag("menu_history"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Match History",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenSettings() }
                            .testTag("menu_settings"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Settings",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog for Player vs Bot setup
    if (showPvBotDialog) {
        AlertDialog(
            onDismissRequest = { showPvBotDialog = false },
            title = {
                Text("Player vs Bot Settings", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Choose your color:")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        FilterChip(
                            selected = selectedPlayerColor == PieceColor.WHITE,
                            onClick = { selectedPlayerColor = PieceColor.WHITE },
                            label = { Text("White") }
                        )
                        FilterChip(
                            selected = selectedPlayerColor == PieceColor.BLACK,
                            onClick = { selectedPlayerColor = PieceColor.BLACK },
                            label = { Text("Black") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPvBotDialog = false
                        viewModel.startPlayerVsBot(color = selectedPlayerColor)
                        onStartGame()
                    }
                ) {
                    Text("Start Game")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPvBotDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog for Helper Bot setup
    if (showHelperBotDialog) {
        AlertDialog(
            onDismissRequest = { showHelperBotDialog = false },
            title = {
                Text("Helper Bot Settings", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Which color should the bot play?")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        FilterChip(
                            selected = selectedHelperColor == PieceColor.WHITE,
                            onClick = { selectedHelperColor = PieceColor.WHITE },
                            label = { Text("White") }
                        )
                        FilterChip(
                            selected = selectedHelperColor == PieceColor.BLACK,
                            onClick = { selectedHelperColor = PieceColor.BLACK },
                            label = { Text("Black") }
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Auto-play Bot's moves")
                        Switch(
                            checked = helperAutoPlay,
                            onCheckedChange = { helperAutoPlay = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showHelperBotDialog = false
                        viewModel.startHelperBot(
                            botColor = selectedHelperColor,
                            autoPlay = helperAutoPlay
                        )
                        onStartGame()
                    }
                ) {
                    Text("Start Helper Mode")
                }
            },
            dismissButton = {
                TextButton(onClick = { showHelperBotDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
"""

with open("app/src/main/java/com/example/chess/ui/screens/MainMenuScreen.kt", "a") as f:
    f.write(content)
