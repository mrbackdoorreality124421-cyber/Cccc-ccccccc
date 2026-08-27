package com.example.chess.engine

import com.example.chess.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

class AlphaBetaEngine {

    private val pawnTable = intArrayOf(
        0,  0,  0,  0,  0,  0,  0,  0,
        50, 50, 50, 50, 50, 50, 50, 50,
        10, 10, 20, 30, 30, 20, 10, 10,
        5,  5, 10, 25, 25, 10,  5,  5,
        0,  0,  0, 20, 20,  0,  0,  0,
        5, -5,-10,  0,  0,-10, -5,  5,
        5, 10, 10,-20,-20, 10, 10,  5,
        0,  0,  0,  0,  0,  0,  0,  0
    )

    private val knightTable = intArrayOf(
        -50,-40,-30,-30,-30,-30,-40,-50,
        -40,-20,  0,  0,  0,  0,-20,-40,
        -30,  0, 10, 15, 15, 10,  0,-30,
        -30,  5, 15, 20, 20, 15,  5,-30,
        -30,  0, 15, 20, 20, 15,  0,-30,
        -30,  5, 10, 15, 15, 10,  5,-30,
        -40,-20,  0,  5,  5,  0,-20,-40,
        -50,-40,-30,-30,-30,-30,-40,-50
    )

    private val bishopTable = intArrayOf(
        -20,-10,-10,-10,-10,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  5, 10, 10,  5,  0,-10,
        -10,  5,  5, 10, 10,  5,  5,-10,
        -10,  0, 10, 10, 10, 10,  0,-10,
        -10, 10, 10, 10, 10, 10, 10,-10,
        -10,  5,  0,  0,  0,  0,  5,-10,
        -20,-10,-10,-10,-10,-10,-10,-20
    )

    private val rookTable = intArrayOf(
        0,  0,  0,  0,  0,  0,  0,  0,
        5, 10, 10, 10, 10, 10, 10,  5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        0,  0,  0,  5,  5,  0,  0,  0
    )

    private val queenTable = intArrayOf(
        -20,-10,-10, -5, -5,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  5,  5,  5,  5,  0,-10,
        -5,  0,  5,  5,  5,  5,  0, -5,
        0,  0,  5,  5,  5,  5,  0, -5,
        -10,  5,  5,  5,  5,  5,  0,-10,
        -10,  0,  5,  0,  0,  0,  0,-10,
        -20,-10,-10, -5, -5,-10,-10,-20
    )

    private val kingTableMidgame = intArrayOf(
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -20,-30,-30,-40,-40,-30,-30,-20,
        -10,-20,-20,-20,-20,-20,-20,-10,
        20, 20,  0,  0,  0,  0, 20, 20,
        20, 30, 10,  0,  0, 10, 30, 20
    )

    private var nodesEvaluated = 0L

    suspend fun findBestMove(
        position: ChessPosition,
        maxDepth: Int = 7,
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

        nodesEvaluated = 0L
        var bestMoveOverall = legalMoves.first()
        var bestScoreOverall = -999999

        // Iterative Deepening
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
            return quiescence(pos, alpha, beta, 4)
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
            val baseVal = piece.type.baseValue
            val rank = i / 8
            val file = i % 8

            // Positional square table bonus
            val tableIdx = if (piece.color == PieceColor.WHITE) {
                (7 - rank) * 8 + file
            } else {
                rank * 8 + file
            }

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
                score += 8000 + (move.promotion.baseValue)
            }
            if (move.isCastleKingside || move.isCastleQueenside) {
                score += 500
            }
            score
        }
    }
}
