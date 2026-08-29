package com.example.chess.model

data class Square(
    val file: Int, // 0 = 'a', 7 = 'h'
    val rank: Int  // 0 = '1', 7 = '8'
) {
    val index: Int
        get() = rank * 8 + file

    val isValid: Boolean
        get() = file in 0..7 && rank in 0..7

    val algebraic: String
        get() {
            require(isValid) { "Invalid square: file=$file rank=$rank" }
            val f = ('a'.code + file).toChar()
            val r = rank + 1
            return "$f$r"
        }

    val isLightSquare: Boolean
        get() = (file + rank) % 2 != 0

    companion object {
        fun fromIndex(index: Int): Square {
            require(index in 0..63) { "Square index out of bounds: $index" }
            return Square(file = index % 8, rank = index / 8)
        }

        /** Parses exactly one algebraic chess square such as e4. */
        fun fromAlgebraic(str: String): Square? {
            if (str.length != 2) return null
            val file = str[0].lowercaseChar() - 'a'
            val rank = str[1].digitToIntOrNull()?.minus(1) ?: return null
            if (file !in 0..7 || rank !in 0..7) return null
            return Square(file, rank)
        }
    }

    override fun toString(): String = algebraic
}
