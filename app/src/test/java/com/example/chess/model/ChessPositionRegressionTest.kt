package com.example.chess.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChessPositionRegressionTest {

    @Test
    fun initialPositionHasTwentyLegalMoves() {
        val position = ChessPosition.initial()

        assertEquals(20, position.generateLegalMoves().size)
        assertEquals("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", position.toFen())
    }

    @Test
    fun squareParserRejectsTrailingCharacters() {
        assertEquals(Square(4, 3), Square.fromAlgebraic("e4"))
        assertNull(Square.fromAlgebraic("e4x"))
        assertNull(Square.fromAlgebraic("e"))
        assertNull(Square.fromAlgebraic("e9"))
    }

    @Test
    fun fenParserRejectsMalformedBoardAndMissingKing() {
        assertNull(ChessPosition.fromFen("8/8/8/8/8/8/8/9 w - - 0 1"))
        assertNull(ChessPosition.fromFen("8/8/8/8/8/8/8/7k w - - 0 1"))
        assertNull(ChessPosition.fromFen("8/8/8/8/8/8/7K/7k x - - 0 1"))
    }

    @Test
    fun fenRoundTripPreservesValidPosition() {
        val fen = "r3k2r/ppp2ppp/2n5/8/8/2N5/PPP2PPP/R3K2R w KQkq - 3 12"
        val position = ChessPosition.fromFen(fen)

        assertEquals(fen, position?.toFen())
    }

    @Test
    fun castlingRequiresRookAndClearSafeTransitSquares() {
        val position = ChessPosition.fromFen("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1")!!
        val sans = position.generateLegalMoves().map { it.san }

        assertTrue(sans.contains("O-O"))
        assertTrue(sans.contains("O-O-O"))
    }

    @Test
    fun enPassantCaptureRemovesTheCorrectPawn() {
        val position = ChessPosition.fromFen("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1")!!
        val move = position.generateLegalMoves().single { it.uci == "e5d6" }
        val next = position.makeMove(move)

        assertEquals(PieceType.PAWN, next.pieceAt(Square.fromAlgebraic("d6")!!)?.type)
        assertNull(next.pieceAt(Square.fromAlgebraic("d5")!!))
    }

    @Test
    fun promotionGeneratesExactlyFourChoices() {
        val position = ChessPosition.fromFen("4k3/P7/8/8/8/8/8/4K3 w - - 0 1")!!
        val promotions = position.generateLegalMoves().filter { it.from == Square(0, 6) }

        assertEquals(4, promotions.size)
        assertEquals(setOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT), promotions.mapNotNull { it.promotion }.toSet())
    }

    @Test
    fun enemyKingCanNeverBeCapturedAsALegalMove() {
        val position = ChessPosition.fromFen("4k3/4Q3/8/8/8/8/8/4K3 w - - 0 1")!!
        assertFalse(position.generateLegalMoves().any { it.capturedPiece == PieceType.KING })
    }

    @Test
    fun oppositeColorBishopsRemainTechnicallyPlayable() {
        val position = ChessPosition.fromFen("4k3/8/8/8/8/8/3b4/2B1K3 w - - 0 1")!!
        assertFalse(position.isInsufficientMaterial())
    }

    @Test
    fun sameColorBishopsAreInsufficientMaterial() {
        val position = ChessPosition.fromFen("4k3/8/8/8/8/2b5/3b4/4K3 w - - 0 1")!!
        assertTrue(position.isInsufficientMaterial())
    }
}
