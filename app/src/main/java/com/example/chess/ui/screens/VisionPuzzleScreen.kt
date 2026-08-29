package com.example.chess.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.chess.data.SecurePreferences
import com.example.chess.network.GroqApiClient
import com.example.chess.network.GroqChatRequest
import com.example.chess.network.GroqContent
import com.example.chess.network.GroqImageUrl
import com.example.chess.network.GroqMessage
import com.example.chess.ui.ChessViewModel
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

@Composable
fun VisionPuzzleScreen(
    viewModel: ChessViewModel,
    onNavigateToEditor: () -> Unit,
    onNavigateToAnalysis: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        selectedImageUri = uri
        uri?.let {
            try {
                selectedBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, it)
                    ImageDecoder.decodeBitmap(source)
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                }
            } catch (e: Exception) {
                errorMessage = "Failed to load image"
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("AI Vision Puzzle", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        if (selectedBitmap != null) {
            Image(
                bitmap = selectedBitmap!!.asImageBitmap(),
                contentDescription = "Selected Puzzle",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { launcher.launch("image/*") }, enabled = !isLoading) {
            Text("Select Image")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator()
            Text("Analyzing with Groq Vision...", modifier = Modifier.padding(top = 8.dp))
        }

        errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
            Button(onClick = onNavigateToEditor) {
                Text("Manually Setup Board")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                val apiKey = SecurePreferences.getGroqApiKey(context)
                if (apiKey.isNullOrBlank()) {
                    errorMessage = "Groq API Key not found. Please set it in Settings."
                    return@Button
                }
                
                selectedBitmap?.let { bmp ->
                    isLoading = true
                    errorMessage = null
                    coroutineScope.launch {
                        try {
                            val outputStream = ByteArrayOutputStream()
                            bmp.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                            val base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                            val dataUri = "data:image/jpeg;base64,$base64Image"

                            val request = GroqChatRequest(
                                messages = listOf(
                                    GroqMessage(
                                        role = "user",
                                        content = listOf(
                                            GroqContent(type = "text", text = "You are a chess master. I will provide an image of a chess board. Your ONLY output should be the exact FEN string representing the position on the board. Do not include any other text, explanation, or formatting. Just the FEN string. Assume it is white's turn to move if not clear. Example output: rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"),
                                            GroqContent(type = "image_url", image_url = GroqImageUrl(url = dataUri))
                                        )
                                    )
                                )
                            )

                            val response = GroqApiClient.apiService.getChatCompletion("Bearer $apiKey", request)
                            val fen = response.choices.firstOrNull()?.message?.content?.trim()
                            
                            if (fen != null) {
                                // Load FEN and go to analysis
                                val success = viewModel.loadFen(fen)
                                if (success) {
                                    onNavigateToAnalysis()
                                } else {
                                    errorMessage = "Invalid FEN returned by AI: $fen"
                                }
                            } else {
                                errorMessage = "Failed to extract FEN from image."
                            }
                        } catch (e: Exception) {
                            errorMessage = "API Error: ${e.message}"
                        } finally {
                            isLoading = false
                        }
                    }
                }
            },
            enabled = selectedBitmap != null && !isLoading,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Extract FEN & Analyze")
        }
    }
}
