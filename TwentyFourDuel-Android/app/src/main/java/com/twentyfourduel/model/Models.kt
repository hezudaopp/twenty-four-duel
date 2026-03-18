package com.twentyfourduel.model

import java.util.UUID

enum class GamePhase { WELCOME, HISTORY, PLAYING, ROUND_OVER, GAME_OVER }

enum class Difficulty(val label: String, val maxNumber: Int) {
    EASY("简单 1-9", 9),
    NORMAL("普通 1-13", 13)
}

enum class CardSuit(val symbol: String, val isRed: Boolean) {
    SPADE("♠", false),
    HEART("♥", true),
    DIAMOND("♦", true),
    CLUB("♣", false);

    companion object {
        val random: CardSuit get() = entries.random()
    }
}

sealed class Token {
    data class Number(val value: Int, val cardIndex: Int) : Token()
    data class Op(val op: String) : Token()
    data class Paren(val paren: String) : Token()

    val display: String
        get() = when (this) {
            is Number -> "$value"
            is Op -> when (op) {
                "*" -> "×"; "/" -> "÷"; "-" -> "−"; else -> op
            }
            is Paren -> paren
        }

    val evalString: String
        get() = when (this) {
            is Number -> "$value"
            is Op -> op
            is Paren -> paren
        }

    val isNumber: Boolean get() = this is Number
}

data class PlayerState(
    var score: Int = 0,
    var tokens: MutableList<Token> = mutableListOf(),
    var usedCards: BooleanArray = BooleanArray(4) { false },
    var passed: Boolean = false,
    var feedback: String? = null,
    var shakeCount: Int = 0,
    var showSuccess: Boolean = false,
    var wantsToEnd: Boolean = false,
    var wantsToSkip: Boolean = false
) {
    fun copy(): PlayerState = PlayerState(
        score, tokens.toMutableList(), usedCards.copyOf(),
        passed, feedback, shakeCount, showSuccess, wantsToEnd, wantsToSkip
    )
}

enum class RoundResult { CORRECT, WRONG, TIMEOUT, SKIPPED, NO_ANSWER }

data class RoundRecord(
    val id: String = UUID.randomUUID().toString(),
    val roundNumber: Int,
    val numbers: List<Int>,
    val player1Expression: String,
    val player2Expression: String,
    val player1Result: RoundResult,
    val player2Result: RoundResult,
    val winnerIndex: Int?,
    val solutions: List<String>
)

data class GameRecord(
    val id: String = UUID.randomUUID().toString(),
    val date: Long = System.currentTimeMillis(),
    val difficulty: String,
    val totalRounds: Int,
    val timeLimit: Int,
    val player1Score: Int,
    val player2Score: Int,
    val rounds: List<RoundRecord>
) {
    val winnerText: String
        get() = when {
            player1Score > player2Score -> "玩家一胜"
            player2Score > player1Score -> "玩家二胜"
            else -> "平局"
        }
}
