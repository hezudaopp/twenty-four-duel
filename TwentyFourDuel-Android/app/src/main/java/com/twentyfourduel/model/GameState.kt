package com.twentyfourduel.model

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.twentyfourduel.sound.SoundManager
import kotlin.math.abs

class GameState : ViewModel() {
    var phase by mutableStateOf(GamePhase.WELCOME)
    var difficulty by mutableStateOf(Difficulty.NORMAL)
    var totalRounds by mutableIntStateOf(10)
    var timeLimit by mutableIntStateOf(60)
    var currentRound by mutableIntStateOf(0)
    var numbers = mutableStateListOf<Int>()
    var suits = mutableStateListOf<CardSuit>()
    var solutions = mutableStateListOf<String>()
    var timeLeft by mutableIntStateOf(60)
    var roundWinner by mutableStateOf<Int?>(null)

    var player0 by mutableStateOf(PlayerState())
    var player1 by mutableStateOf(PlayerState())

    fun getPlayer(i: Int) = if (i == 0) player0 else player1
    private fun setPlayer(i: Int, s: PlayerState) { if (i == 0) player0 = s else player1 = s }

    private var roundRecords = mutableListOf<RoundRecord>()
    private var playerExpressions = arrayOf("", "")
    private var playerResults = arrayOf(RoundResult.NO_ANSWER, RoundResult.NO_ANSWER)

    var soundManager: SoundManager? = null
    var vibrator: Vibrator? = null
    var historyStore: HistoryStore? = null

    fun startGame() {
        currentRound = 0
        player0 = PlayerState()
        player1 = PlayerState()
        roundRecords.clear()
        startRound()
    }

    fun startRound() {
        currentRound++
        generateNumbers()
        for (i in 0..1) {
            val p = getPlayer(i).copy()
            p.tokens.clear()
            p.usedCards = BooleanArray(4) { false }
            p.passed = false
            p.feedback = null
            p.showSuccess = false
            p.wantsToEnd = false
            p.wantsToSkip = false
            setPlayer(i, p)
        }
        roundWinner = null
        playerExpressions = arrayOf("", "")
        playerResults = arrayOf(RoundResult.NO_ANSWER, RoundResult.NO_ANSWER)
        phase = GamePhase.PLAYING
        timeLeft = timeLimit
    }

    fun tick() {
        if (phase != GamePhase.PLAYING || timeLimit <= 0) return
        timeLeft--
        if (timeLeft <= 0) {
            soundManager?.play("timeout")
            endRound(null)
        } else if (timeLeft <= 10) {
            soundManager?.play("tick")
        }
    }

    fun endRound(winner: Int?) {
        if (phase != GamePhase.PLAYING) return
        phase = GamePhase.ROUND_OVER
        roundWinner = winner
        if (winner != null) {
            val p = getPlayer(winner).copy()
            p.score++
            setPlayer(winner, p)
        }
        if (winner == null) {
            val isSkip = player0.wantsToSkip && player1.wantsToSkip
            val isTimeout = timeLimit > 0 && timeLeft <= 0
            for (i in 0..1) {
                if (playerExpressions[i].isEmpty()) {
                    playerExpressions[i] = getPlayer(i).tokens.joinToString("") { it.display }
                }
                if (playerResults[i] == RoundResult.NO_ANSWER) {
                    playerResults[i] = when {
                        isSkip -> RoundResult.SKIPPED
                        isTimeout -> RoundResult.TIMEOUT
                        else -> RoundResult.NO_ANSWER
                    }
                }
            }
        }

        roundRecords.add(
            RoundRecord(
                roundNumber = currentRound,
                numbers = numbers.toList(),
                player1Expression = playerExpressions[0],
                player2Expression = playerExpressions[1],
                player1Result = playerResults[0],
                player2Result = playerResults[1],
                winnerIndex = winner,
                solutions = solutions.take(3)
            )
        )
    }

    fun advanceAfterRound() {
        if (phase != GamePhase.ROUND_OVER) return
        if (currentRound >= totalRounds) {
            endGame()
        } else {
            startRound()
        }
    }

    fun endGame() {
        phase = GamePhase.GAME_OVER
        soundManager?.play("gameover")
        saveGameRecord()
    }

    private fun recordCurrentRoundIfNeeded() {
        if (roundRecords.any { it.roundNumber == currentRound }) return
        if (numbers.isEmpty()) return
        for (i in 0..1) {
            if (playerExpressions[i].isEmpty()) {
                playerExpressions[i] = getPlayer(i).tokens.joinToString("") { it.display }
            }
        }
        roundRecords.add(
            RoundRecord(
                roundNumber = currentRound,
                numbers = numbers.toList(),
                player1Expression = playerExpressions[0],
                player2Expression = playerExpressions[1],
                player1Result = playerResults[0],
                player2Result = playerResults[1],
                winnerIndex = null,
                solutions = solutions.take(3)
            )
        )
    }

    private fun saveGameRecord() {
        historyStore?.save(
            GameRecord(
                difficulty = difficulty.label,
                totalRounds = totalRounds,
                timeLimit = timeLimit,
                player1Score = player0.score,
                player2Score = player1.score,
                rounds = roundRecords.toList()
            )
        )
    }

    fun backToWelcome() {
        phase = GamePhase.WELCOME
    }

    // Input validation
    fun canInputNumber(player: Int): Boolean {
        val tokens = getPlayer(player).tokens
        val last = tokens.lastOrNull() ?: return true
        return when (last) {
            is Token.Number -> false
            is Token.Op -> true
            is Token.Paren -> last.paren == "("
        }
    }

    fun canInputOp(player: Int): Boolean {
        val last = getPlayer(player).tokens.lastOrNull() ?: return false
        return when (last) {
            is Token.Number -> true
            is Token.Op -> false
            is Token.Paren -> last.paren == ")"
        }
    }

    fun canInputOpenParen(player: Int): Boolean {
        val tokens = getPlayer(player).tokens
        val openCount = tokens.count { it is Token.Paren && it.paren == "(" }
        if (openCount >= 3) return false
        val last = tokens.lastOrNull() ?: return true
        return when (last) {
            is Token.Number -> false
            is Token.Op -> true
            is Token.Paren -> last.paren == "("
        }
    }

    fun canInputCloseParen(player: Int): Boolean {
        val tokens = getPlayer(player).tokens
        val openCount = tokens.count { it is Token.Paren && it.paren == "(" }
        val closeCount = tokens.count { it is Token.Paren && it.paren == ")" }
        if (openCount <= closeCount) return false
        val last = tokens.lastOrNull() ?: return false
        return when (last) {
            is Token.Number -> true
            is Token.Op -> false
            is Token.Paren -> last.paren == ")"
        }
    }

    // Player actions
    fun handleNumber(player: Int, cardIndex: Int) {
        if (phase != GamePhase.PLAYING) return
        val p = getPlayer(player)
        if (p.usedCards[cardIndex] || !canInputNumber(player)) return
        val np = p.copy()
        np.passed = false
        np.tokens.add(Token.Number(numbers[cardIndex], cardIndex))
        np.usedCards[cardIndex] = true
        np.feedback = null
        setPlayer(player, np)
        soundManager?.play("tap")
        hapticLight()
    }

    fun handleOp(player: Int, op: String) {
        if (phase != GamePhase.PLAYING) return
        when (op) {
            "(" -> if (!canInputOpenParen(player)) return
            ")" -> if (!canInputCloseParen(player)) return
            else -> if (!canInputOp(player)) return
        }
        val np = getPlayer(player).copy()
        np.passed = false
        np.tokens.add(if (op == "(" || op == ")") Token.Paren(op) else Token.Op(op))
        np.feedback = null
        setPlayer(player, np)
        soundManager?.play("tap")
        hapticLight()
    }

    fun handleBackspace(player: Int) {
        if (phase != GamePhase.PLAYING) return
        val p = getPlayer(player)
        if (p.tokens.isEmpty()) return
        val np = p.copy()
        np.passed = false
        val removed = np.tokens.removeAt(np.tokens.lastIndex)
        if (removed is Token.Number) np.usedCards[removed.cardIndex] = false
        np.feedback = null
        setPlayer(player, np)
        hapticLight()
    }

    fun handleClear(player: Int) {
        if (phase != GamePhase.PLAYING) return
        val np = getPlayer(player).copy()
        np.passed = false
        np.tokens.clear()
        np.usedCards = BooleanArray(4) { false }
        np.feedback = null
        setPlayer(player, np)
        hapticLight()
    }

    fun handleSubmit(player: Int) {
        if (phase != GamePhase.PLAYING) return
        val p = getPlayer(player)
        val numCount = p.tokens.count { it.isNumber }
        if (numCount != 4 || p.usedCards.any { !it }) {
            soundManager?.play("wrong")
            showError(player, "请使用全部 4 个数字")
            return
        }
        val exprStr = p.tokens.joinToString("") { it.evalString }
        val v = ExpressionEvaluator.evaluate(exprStr)
        if (v == null || !v.isFinite()) {
            soundManager?.play("wrong")
            showError(player, "算式格式错误")
            return
        }
        val displayExpr = p.tokens.joinToString("") { it.display }
        playerExpressions[player] = displayExpr

        if (abs(v - 24.0) < 1e-9) {
            playerResults[player] = RoundResult.CORRECT
            val np = p.copy(); np.showSuccess = true; setPlayer(player, np)
            soundManager?.play("correct")
            hapticSuccess()
            endRound(player)
        } else {
            playerResults[player] = RoundResult.WRONG
            val display = if (v == v.toLong().toDouble() && abs(v) < 1e6)
                v.toLong().toString() else String.format("%.2f", v)
            soundManager?.play("wrong")
            showError(player, "= $display，不等于 24")
        }
    }

    fun handleRequestSkip(player: Int) {
        if (phase != GamePhase.PLAYING || timeLimit != 0) return
        val np = getPlayer(player).copy()
        np.wantsToSkip = !np.wantsToSkip
        np.feedback = if (np.wantsToSkip) "已请求跳过，等待对方同意..." else null
        setPlayer(player, np)
        if (np.wantsToSkip) hapticLight()
        if (player0.wantsToSkip && player1.wantsToSkip) {
            hapticSuccess()
            endRound(null)
        }
    }

    fun handleRequestEnd(player: Int) {
        if (phase != GamePhase.PLAYING && phase != GamePhase.ROUND_OVER) return
        val np = getPlayer(player).copy()
        np.wantsToEnd = !np.wantsToEnd
        setPlayer(player, np)
        if (np.wantsToEnd) hapticLight()
        if (player0.wantsToEnd && player1.wantsToEnd) {
            hapticSuccess()
            recordCurrentRoundIfNeeded()
            saveGameRecord()
            backToWelcome()
        }
    }

    private fun showError(player: Int, text: String) {
        val np = getPlayer(player).copy()
        np.feedback = text
        np.shakeCount++
        setPlayer(player, np)
        hapticError()
    }

    private fun generateNumbers() {
        val max = difficulty.maxNumber
        numbers.clear(); suits.clear(); solutions.clear()
        repeat(500) {
            val nums = List(4) { (1..max).random() }
            val sols = Solver.solve24(nums)
            if (sols.isNotEmpty()) {
                numbers.addAll(nums)
                suits.addAll(List(4) { CardSuit.random })
                solutions.addAll(sols)
                return
            }
        }
        numbers.addAll(listOf(1, 2, 3, 4))
        suits.addAll(listOf(CardSuit.SPADE, CardSuit.HEART, CardSuit.DIAMOND, CardSuit.CLUB))
        solutions.addAll(Solver.solve24(listOf(1, 2, 3, 4)))
    }

    private fun hapticLight() {
        try { vibrator?.vibrate(VibrationEffect.createOneShot(20, 60)) } catch (_: Exception) {}
    }
    private fun hapticSuccess() {
        try { vibrator?.vibrate(VibrationEffect.createOneShot(40, 120)) } catch (_: Exception) {}
    }
    private fun hapticError() {
        try { vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 30, 50, 30), -1)) } catch (_: Exception) {}
    }
}
