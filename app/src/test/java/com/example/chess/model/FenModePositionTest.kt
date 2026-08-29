package com.example.chess.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun partialCastlingRightsPreserved() {
        val fen = "r3k2r/8/8/8/8/8/8/R3K2R w Kq - 2 10"
        val position = ChessPosition.fromFen(fen)
        assertNotNull(position)
        assertEquals(PieceColor.WHITE, position!!.activeColor)
        assertEquals("Kq", position.castlingRights.toFen())
        assertEquals(fen, position.toFen())
    }

    @Test
    fun noCastlingAndNoEnPassantPreserved() {
        val fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1"
        val position = ChessPosition.fromFen(fen)
        assertNotNull(position)
        assertEquals(PieceColor.WHITE, position!!.activeColor)
        assertEquals("-", position.castlingRights.toFen())
        assertNull(position.enPassantTarget)
        assertEquals(fen, position.toFen())
    }

    @Test
    fun invalidFenIsRejected() {
        assertNull(ChessPosition.fromFen("8/8/8/8/8/8/8/8 w - - 0 1")) // No kings
        assertNull(ChessPosition.fromFen("4k3/8/8/8/8/8/8/8 w - - 0 1")) // Missing white king
        assertNull(ChessPosition.fromFen("4K3/8/8/8/8/8/8/8 w - - 0 1")) // Missing black king
        assertNull(ChessPosition.fromFen("invalid fen format"))
        assertNull(ChessPosition.fromFen(""))
    }

    @Test
    fun complexMiddlegameFenRoundTrip() {
        val fen = "r1bqk2r/pppp1ppp/2n5/4p3/1b1Pn3/2N2N2/PPP1BPPP/R1BQK2R w KQkq - 0 6"
        val position = ChessPosition.fromFen(fen)
        assertNotNull(position)
        assertEquals(PieceColor.WHITE, position!!.activeColor)
        assertEquals("KQkq", position.castlingRights.toFen())
        assertEquals(fen, position.toFen())
    }
}
