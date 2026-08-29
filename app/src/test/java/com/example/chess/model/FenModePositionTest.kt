package com.example.chess.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class FenModePositionTest {
    @Test
    fun whiteToMovePositionPreservesFenState() {
        val fen = "8/8/8/3k4/8/8/4K3/8 w - - 7 23"
        val position = ChessPosition.fromFen(fen)
        assertNotNull(position)
        assertEquals(PieceColor.WHITE, position!!.activeColor)
        assertEquals(7, position.halfmoveClock)
        assertEquals(23, position.fullmoveNumber)
        assertEquals(fen, position.toFen())
    }

    @Test
    fun blackToMovePreservesCastlingAndEnPassant() {
        val fen = "r3k2r/8/8/3pP3/8/8/8/R3K2R b KQkq e6 0 17"
        val position = ChessPosition.fromFen(fen)
        assertNotNull(position)
        assertEquals(PieceColor.BLACK, position!!.activeColor)
        assertEquals("KQkq", position.castlingRights.toFen())
        assertEquals("e6", position.enPassantTarget?.algebraic)
        assertEquals(fen, position.toFen())
    }

    @Test
    fun invalidFenIsRejected() {
        assertEquals(null, ChessPosition.fromFen("8/8/8/8/8/8/8/8 w - - 0 1"))
    }
}
