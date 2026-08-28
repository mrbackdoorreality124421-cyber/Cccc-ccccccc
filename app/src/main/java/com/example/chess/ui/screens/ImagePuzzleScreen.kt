package com.example.chess.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.chess.ai.GroqVisionService
import com.example.chess.model.FenParser
import com.example.chess.ui.ChessViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePuzzleScreen(viewModel: ChessViewModel, onBack: () -> Unit, onOpenCustomBoard: () -> Unit, onAnalyze: () -> Unit, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val service = remember { GroqVisionService(context) }
    val scope = rememberCoroutineScope()
    var apiKey by remember { mutableStateOf(service.savedKey().orEmpty()) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var detectedFen by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var showKey by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedUri = uri; detectedFen = null
        status = if (uri == null) "No image selected." else "Image selected. Ready to detect position."
    }

    fun detect() {
        val uri = selectedUri ?: run { status = "Select a chess image first."; return }
        if (apiKey.isBlank()) { status = "Groq API key is missing. Use Manual Piece Placement."; return }
        busy = true; status = "Reading board with Groq Vision…"
        scope.launch {
            service.imageToFen(context, uri, apiKey).onSuccess { fen -> detectedFen = fen; status = "Position detected. Review before analysis." }
                .onFailure { status = it.message ?: "Vision detection failed." }
            busy = false
        }
    }

    Scaffold(modifier.fillMaxSize(), topBar = { TopAppBar(title = { Text("Image Puzzle") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column { Text("Chess Image → FEN", style = MaterialTheme.typography.titleLarge); Text("Groq Vision reads the actual board and pieces.", style = MaterialTheme.typography.bodySmall) }
                    }
                    OutlinedTextField(value = apiKey, onValueChange = { apiKey = it; service.saveKey(it) }, modifier = Modifier.fillMaxWidth(), label = { Text("Groq API Key") }, leadingIcon = { Icon(Icons.Default.Key, null) }, visualTransformation = if (showKey) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(), singleLine = true, trailingIcon = { TextButton(onClick = { showKey = !showKey }) { Text(if (showKey) "Hide" else "Show") } })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            if (apiKey.isBlank()) status = "Enter your Groq API key first."
                            else { busy = true; scope.launch { service.testConnection(apiKey).onSuccess { status = it }.onFailure { status = it.message }; busy = false } }
                        }, enabled = !busy) { Text("Test Connection") }
                        TextButton(onClick = { apiKey = ""; service.clearKey(); status = "Saved key removed." }) { Text("Clear Key") }
                    }
                }
            }
            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { picker.launch("image/*") }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Image, null); Spacer(Modifier.width(8.dp)); Text(if (selectedUri == null) "Select Puzzle Image" else "Choose Another Image") }
                    Button(onClick = ::detect, enabled = selectedUri != null && apiKey.isNotBlank() && !busy, modifier = Modifier.fillMaxWidth()) { if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Icon(Icons.Default.AutoAwesome, null); Spacer(Modifier.width(8.dp)); Text("Detect Position") }
                }
            }
            detectedFen?.let { fen ->
                Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Detected Position — Review", style = MaterialTheme.typography.titleMedium)
                        var editableFen by remember(fen) { mutableStateOf(fen) }
                        OutlinedTextField(value = editableFen, onValueChange = { editableFen = it; detectedFen = it }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { if (FenParser.parse(editableFen).isSuccess && viewModel.loadFen(editableFen)) onAnalyze() else status = "FEN is invalid. Edit it or use manual setup." }, modifier = Modifier.weight(1f)) { Text("Confirm & Analyze") }
                            OutlinedButton(onClick = onOpenCustomBoard, modifier = Modifier.weight(1f)) { Text("Edit Manually") }
                        }
                    }
                }
            }
            status?.let { Text(it, color = if (it.contains("failed", true) || it.contains("missing", true) || it.contains("invalid", true)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
            OutlinedButton(onClick = onOpenCustomBoard, modifier = Modifier.fillMaxWidth()) { Text("Manual Piece Placement") }
        }
    }
}
