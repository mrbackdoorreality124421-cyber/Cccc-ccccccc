package com.example.chess.ui

import android.content.Context
import com.example.chess.engine.OexEngineInfo
import com.example.chess.engine.OexEngineManager
import com.example.chess.model.ChessMove
import com.example.chess.model.ChessPosition
import com.example.chess.model.Square
import com.example.chess.model.PieceType
import com.example.chess.model.PieceColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Standalone Stockfish-backed session for playing directly from a custom position. */
class CustomPositionBotSession(context: Context) {
    private val engine = OexEngineManager(context.applicationContext)
    private var selectedEngine: OexEngineInfo? = null

    suspend fun start(): Result<String> = withContext(Dispatchers.IO) {
        val engines = engine.discoverEngines()
        val stockfish = engines.firstOrNull { it.isStockfish && it.executablePath != null }
            ?: return@withContext Result.failure(IllegalStateException("Stockfish engine was not found. Open Engine Discovery and import/select Stockfish first."))
        if (!engine.startEngine(stockfish)) {
            return@withContext Result.failure(IllegalStateException("Stockfish could not be started. Check the engine installation or permissions."))
        }
        selectedEngine = stockfish
        Result.success(stockfish.name)
    }

    suspend fun bestMove(position: ChessPosition, history: List<ChessMove>, depth: Int, moveTimeMs: Int): Result<EngineMove> = withContext(Dispatchers.IO) {
        if (!engine.isRunning) {
            val started = start()
            if (started.isFailure) return@withContext Result.failure(started.exceptionOrNull()!!)
        }
        val result = engine.findBestMove(
            fen = position.toFen(),
            movesUci = history.map { it.uci },
            depth = depth.coerceIn(1, 30),
            moveTimeMs = moveTimeMs.coerceAtLeast(250)
        )
        val uci = result?.bestMoveUci ?: return@withContext Result.failure(IllegalStateException("Stockfish did not return a best move."))
        val move = parseUci(uci, position) ?: return@withContext Result.failure(IllegalStateException("Stockfish returned an illegal move: $uci"))
        Result.success(EngineMove(move, result.scoreCp, result.mateIn))
    }

    fun stop() = engine.stopEngine()

    private fun parseUci(uci: String, position: ChessPosition): ChessMove? {
        if (uci.length < 4) return null
        val from = Square.fromAlgebraic(uci.substring(0, 2)) ?: return null
        val to = Square.fromAlgebraic(uci.substring(2, 4)) ?: return null
        val promotion = when (uci.getOrNull(4)?.lowercaseChar()) {
            'q' -> PieceType.QUEEN
            'r' -> PieceType.ROOK
            'b' -> PieceType.BISHOP
            'n' -> PieceType.KNIGHT
            else -> null
        }
        return position.generateLegalMoves().firstOrNull { move ->
            move.from == from && move.to == to && (promotion == null || move.promotion == promotion)
        }
    }
}

data class EngineMove(
    val move: ChessMove,
    val scoreCp: Int,
    val mateIn: Int?
)
