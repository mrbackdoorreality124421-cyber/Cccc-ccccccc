package com.example.chess.engine

import com.example.chess.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class AlphaBetaEngine {

    // Piece-Square Tables (oriented from rank 7 down to rank 0 for White)
    // Index = (7 - rank) * 8 + file for White, and rank * 8 + file for Black
    private val pawnTable = intArrayOf(
        0,   0,   0,   0,   0,   0,   0,   0,   // Rank 8 (Promoted)
        50,  50,  50,  50,  50,  50,  50,  50,  // Rank 7
        10,  15,  25,  35,  35,  25,  15,  10,  // Rank 6
        5,   10,  20,  30,  30,  20,  10,   5,  // Rank 5
        0,    5,  15,  25,  25,  15,   5,   0,  // Rank 4
        0,    0,   5,  20,  20,   5,   0,   0,  // Rank 3
        5,   10,  10, -10, -10,  10,  10,   5,  // Rank 2 (Base rank pawns)
        0,    0,   0,   0,   0,   0,   0,   0   // Rank 1
    )

    private val knightTable = intArrayOf(
        -50, -40, -30, -30, -30, -30, -40, -50,
        -40, -20,   0,   5,   5,   0, -20, -40,
        -30,   5,  15,  20,  20,  15,   5, -30,
        -30,  10,  20,  25,  25,  20,  10, -30,
        -30,  10,  20,  25,  25,  20,  10, -30,
        -30,   5,  15,  20,  20,  15,   5, -30,
        -40, -20,   0,   5,   5,   0, -20, -40,
        -50, -35, -20, -20, -20, -20, -35, -50
    )

    private val bishopTable = intArrayOf(
        -20, -10, -10, -10, -10, -10, -10, -20,
        -10,   5,   0,   0,   0,   0,   5, -10,
        -10,  10,  10,  15,  15,  10,  10, -10,
        -10,   5,  15,  20,  20,  15,   5, -10,
        -10,   5,  15,  20,  20,  15,   5, -10,
        -10,  10,  10,  15,  15,  10,  10, -10,
        -10,   5,   0,   0,   0,   0,   5, -10,
        -20, -10, -10, -10, -10, -10, -10, -20
    )

    private val rookTable = intArrayOf(
        0,   0,   0,   5,   5,   0,   0,   0,
        15,  20,  20,  20,  20,  20,  20,  15,  // 7th rank dominance
        -5,   0,   0,   0,   0,   0,   0,  -5,
        -5,   0,   0,   0,   0,   0,   0,  -5,
        -5,   0,   0,   0,   0,   0,   0,  -5,
        -5,   0,   0,   0,   0,   0,   0,  -5,
        -5,   0,   0,   0,   0,   0,   0,  -5,
        0,   0,   5,  10,  10,   5,   0,   0
    )

    private val queenTable = intArrayOf(
        -20, -10, -10,  -5,  -5, -10, -10, -20,
        -10,   0,   5,   0,   0,   0,   0, -10,
        -10,   5,   5,   5,   5,   5,   0, -10,
         -5,   0,   5,  10,  10,   5,   0,  -5,
          0,   0,   5,  10,  10,   5,   0,  -5,
        -10,   5,   5,   5,   5,   5,   0, -10,
        -10,   0,   5,   0,   0,   0,   0, -10,
        -20, -10, -10,  -5,  -5, -10, -10, -20
    )

    private val kingTableMidgame = intArrayOf(
        -40, -50, -50, -50, -50, -50, -50, -40,
        -30, -40, -40, -50, -50, -40, -40, -30,
        -30, -40, -40, -50, -50, -40, -40, -30,
        -30, -35, -35, -40, -40, -35, -35, -30,
        -20, -30, -30, -35, -35, -30, -30, -20,
        -10, -20, -20, -20, -20, -20, -20, -10,
         15,  15,   0,   0,   0,   0,  15,  15,
         20,  30,  10,   0,   0,  10,  30,  20   // Castled king safety (g1/c1)
    )

    // Master Opening Book for strong, classical beginnings (UCI notation)
    private val openingBook = mapOf(
        "" to listOf("e2e4", "d2d4", "g1f3", "c2c4"),
        // Replies to 1. e4
        "e2e4" to listOf("e7e5", "c7c5", "e7e6", "c7c6"),
        "e2e4 e7e5" to listOf("g1f3", "f1c4", "d2d4", "b1c3"),
        "e2e4 e7e5 g1f3" to listOf("b8c6", "g8f6", "d7d6"),
        "e2e4 e7e5 g1f3 b8c6" to listOf("f1b5", "f1c4", "d2d4"), // Ruy Lopez, Italian, Scotch
        "e2e4 e7e5 g1f3 b8c6 f1b5" to listOf("a7a6", "g8f6"),
        "e2e4 e7e5 g1f3 b8c6 f1c4" to listOf("f8c5", "g8f6"),
        // Sicilian Defense
        "e2e4 c7c5" to listOf("g1f3", "b1c3", "c2c3"),
        "e2e4 c7c5 g1f3" to listOf("d7d6", "e7e6", "b8c6"),
        "e2e4 c7c5 g1f3 d7d6" to listOf("d2d4"),
        // Replies to 1. d4
        "d2d4" to listOf("d7d5", "g8f6", "e7e6"),
        "d2d4 d7d5" to listOf("c2c4", "g1f3", "c1f4"),
        "d2d4 d7d5 c2c4" to listOf("e7e6", "c7c6", "d5c4"), // QGD, Slav, QGA
        "d2d4 g8f6" to listOf("c2c4", "g1f3", "c1g5"),
        "d2d4 g8f6 c2c4" to listOf("e7e6", "g7g6", "c7c5"),
        // English Opening / Reti
        "c2c4" to listOf("e7e5", "c7c5", "g8f6", "e7e6"),
        "g1f3" to listOf("d7d5", "g8f6", "c7c5")
    )

    private var nodesEvaluated = 0L

    suspend fun findBestMove(
        position: ChessPosition,
        moveHistoryUci: List<String> = emptyList(),
        maxDepth: Int = 6,
        timeLimitMs: Int = 1200,
        onProgress: ((EngineEvaluation) -> Unit)? = null
    ): EngineEvaluation = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        val legalMoves = position.generateLegalMoves()

        if (legalMoves.isEmpty()) {
            return@withContext EngineEvaluation(
                bestMove = null,
                scoreCp = if (position.isKingInCheck(position.activeColor)) -30000 else 0,
                depth = 0
            )
        }

        if (legalMoves.size == 1) {
            return@withContext EngineEvaluation(
                bestMove = legalMoves.first(),
                scoreCp = evaluatePosition(position),
                depth = 1
            )
        }

        // 1. Check Opening Book first for instant, grandmaster opening theory
        val historyKey = moveHistoryUci.joinToString(" ").trim()
        val bookCandidates = openingBook[historyKey]
        if (!bookCandidates.isNullOrEmpty()) {
            val validBookMoves = bookCandidates.mapNotNull { uci ->
                legalMoves.find { it.uci == uci }
            }
            if (validBookMoves.isNotEmpty()) {
                val selectedBookMove = validBookMoves[Random.nextInt(validBookMoves.size)]
                val eval = EngineEvaluation(
                    bestMove = selectedBookMove,
                    scoreCp = 25,
                    depth = 10,
                    pv = listOf(selectedBookMove.san.ifEmpty { selectedBookMove.uci }),
                    nodesEvaluated = 1,
                    timeElapsedMs = System.currentTimeMillis() - startTime
                )
                onProgress?.invoke(eval)
                return@withContext eval
            }
        }

        nodesEvaluated = 0L
        var bestMoveOverall = legalMoves.first()
        var bestScoreOverall = -999999

        // 2. Iterative Deepening Search with Alpha-Beta Pruning
        for (depth in 1..maxDepth) {
            val elapsed = System.currentTimeMillis() - startTime
            if (depth > 2 && elapsed >= timeLimitMs) break

            var currentDepthBestMove: ChessMove? = null
            var alpha = -999999
            val beta = 999999

            val sortedMoves = orderMoves(legalMoves, position)

            for (move in sortedMoves) {
                val nextPos = position.makeMove(move)
                val score = -alphaBeta(
                    pos = nextPos,
                    depth = depth - 1,
                    alpha = -beta,
                    beta = -alpha,
                    startTime = startTime,
                    timeLimitMs = timeLimitMs
                )

                if (score > alpha) {
                    alpha = score
                    currentDepthBestMove = move
                }

                if (System.currentTimeMillis() - startTime >= timeLimitMs && depth > 2) {
                    break
                }
            }

            if (currentDepthBestMove != null) {
                bestMoveOverall = currentDepthBestMove
                bestScoreOverall = alpha

                val eval = EngineEvaluation(
                    bestMove = bestMoveOverall,
                    scoreCp = bestScoreOverall,
                    mateIn = if (bestScoreOverall > 20000) (30000 - bestScoreOverall + 1) / 2
                    else if (bestScoreOverall < -20000) (-30000 - bestScoreOverall - 1) / 2
                    else null,
                    depth = depth,
                    pv = listOf(bestMoveOverall.san.ifEmpty { bestMoveOverall.uci }),
                    nodesEvaluated = nodesEvaluated,
                    timeElapsedMs = System.currentTimeMillis() - startTime
                )
                onProgress?.invoke(eval)
            }
        }

        EngineEvaluation(
            bestMove = bestMoveOverall,
            scoreCp = bestScoreOverall,
            mateIn = if (bestScoreOverall > 20000) (30000 - bestScoreOverall + 1) / 2
            else if (bestScoreOverall < -20000) (-30000 - bestScoreOverall - 1) / 2
            else null,
            depth = maxDepth,
            pv = listOf(bestMoveOverall.san.ifEmpty { bestMoveOverall.uci }),
            nodesEvaluated = nodesEvaluated,
            timeElapsedMs = System.currentTimeMillis() - startTime
        )
    }

    private fun alphaBeta(
        pos: ChessPosition,
        depth: Int,
        alpha: Int,
        beta: Int,
        startTime: Long,
        timeLimitMs: Int
    ): Int {
        nodesEvaluated++

        if (depth <= 0) {
            return quiescence(pos, alpha, beta, 3)
        }

        if (System.currentTimeMillis() - startTime >= timeLimitMs) {
            return evaluatePosition(pos)
        }

        val legalMoves = pos.generateLegalMoves()
        if (legalMoves.isEmpty()) {
            return if (pos.isKingInCheck(pos.activeColor)) {
                -30000 - depth // Checkmate
            } else {
                0 // Stalemate
            }
        }

        if (pos.halfmoveClock >= 100 || pos.isInsufficientMaterial()) {
            return 0
        }

        val sortedMoves = orderMoves(legalMoves, pos)
        var localAlpha = alpha

        for (move in sortedMoves) {
            val nextPos = pos.makeMove(move)
            val score = -alphaBeta(
                pos = nextPos,
                depth = depth - 1,
                alpha = -beta,
                beta = -localAlpha,
                startTime = startTime,
                timeLimitMs = timeLimitMs
            )

            if (score >= beta) {
                return beta
            }
            if (score > localAlpha) {
                localAlpha = score
            }
        }

        return localAlpha
    }

    private fun quiescence(pos: ChessPosition, alpha: Int, beta: Int, qDepth: Int): Int {
        nodesEvaluated++
        val standPat = evaluatePosition(pos)
        if (qDepth <= 0 || standPat >= beta) return beta

        var localAlpha = max(alpha, standPat)
        val captures = pos.generateLegalMoves().filter { it.isCapture || it.promotion != null }

        val sortedCaptures = orderMoves(captures, pos)
        for (move in sortedCaptures) {
            val nextPos = pos.makeMove(move)
            val score = -quiescence(nextPos, -beta, -localAlpha, qDepth - 1)

            if (score >= beta) return beta
            if (score > localAlpha) localAlpha = score
        }

        return localAlpha
    }

    fun evaluatePosition(pos: ChessPosition): Int {
        var whiteScore = 0
        var blackScore = 0

        for (i in 0..63) {
            val piece = pos.board[i] ?: continue
            val rank = i / 8
            val file = i % 8

            // Positional square table bonus
            val tableIdx = if (piece.color == PieceColor.WHITE) {
                (7 - rank) * 8 + file
            } else {
                rank * 8 + file
            }

            val baseVal = piece.type.baseValue
            val posBonus = when (piece.type) {
                PieceType.PAWN -> pawnTable[tableIdx]
                PieceType.KNIGHT -> knightTable[tableIdx]
                PieceType.BISHOP -> bishopTable[tableIdx]
                PieceType.ROOK -> rookTable[tableIdx]
                PieceType.QUEEN -> queenTable[tableIdx]
                PieceType.KING -> kingTableMidgame[tableIdx]
            }

            val total = baseVal + posBonus
            if (piece.color == PieceColor.WHITE) {
                whiteScore += total
            } else {
                blackScore += total
            }
        }

        val netScore = whiteScore - blackScore
        return if (pos.activeColor == PieceColor.WHITE) netScore else -netScore
    }

    private fun orderMoves(moves: List<ChessMove>, pos: ChessPosition): List<ChessMove> {
        return moves.sortedByDescending { move ->
            var score = 0
            if (move.isCapture) {
                val victimVal = move.capturedPiece?.baseValue ?: 100
                val attackerVal = move.pieceMoved.baseValue
                score += 10000 + (victimVal * 10 - attackerVal)
            }
            if (move.promotion != null) {
                score += 9000 + (move.promotion.baseValue)
            }
            if (move.isCastleKingside || move.isCastleQueenside) {
                score += 600
            }
            // Prefer central pawn advances (e4, d4, e5, d5)
            if (move.pieceMoved == PieceType.PAWN) {
                if (move.to.file in 3..4 && move.to.rank in 3..4) {
                    score += 400
                }
            }
            // Prefer developing knights to c3, f3, c6, f6 over edge squares
            if (move.pieceMoved == PieceType.KNIGHT) {
                if (move.to.file in 2..5 && move.to.rank in 2..5) {
                    score += 350
                } else if (move.to.file == 0 || move.to.file == 7) {
                    score -= 500 // Heavily penalize Na3 / Nh3 / Na6 / Nh6 on early moves!
                }
            }
            score
        }
    }
}
