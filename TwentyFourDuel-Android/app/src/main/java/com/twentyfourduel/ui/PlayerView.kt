package com.twentyfourduel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twentyfourduel.model.GamePhase
import com.twentyfourduel.model.GameState
import com.twentyfourduel.model.Token

@Composable
fun PlayerView(game: GameState, playerIndex: Int, modifier: Modifier = Modifier) {
    val player = game.getPlayer(playerIndex)
    val accent = if (playerIndex == 0) AppColors.p1Accent else AppColors.p2Accent
    val name = if (playerIndex == 0) "玩家一" else "玩家二"
    val isRoundOver = game.phase == GamePhase.ROUND_OVER
    val otherWantsEnd = game.getPlayer(1 - playerIndex).wantsToEnd

    Box(modifier = modifier.fillMaxWidth().background(
        Brush.radialGradient(listOf(accent.copy(alpha = 0.025f), Color.Transparent), radius = 400f)
    )) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(4.dp))

            // Info bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(accent))
                Spacer(Modifier.width(6.dp))
                Text(name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.width(6.dp))
                EndGameButton(player.wantsToEnd) { game.handleRequestEnd(playerIndex) }
                if (otherWantsEnd && !player.wantsToEnd) {
                    Spacer(Modifier.width(4.dp))
                    Text("对方请求结束", fontSize = 11.sp, color = AppColors.gold)
                }
                Spacer(Modifier.weight(1f))
                if (game.timeLimit == 0) {
                    SkipButton(game, playerIndex)
                    Spacer(Modifier.width(4.dp))
                }
                Text("${player.score}", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = accent)
            }

            // Round / Timer bar
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "第 ${game.currentRound} 轮 / 共 ${game.totalRounds} 轮",
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppColors.textDim
                )
                Spacer(Modifier.width(12.dp))
                Text("⏱", fontSize = 11.sp, color = AppColors.textDim)
                Spacer(Modifier.width(4.dp))
                if (game.timeLimit > 0) {
                    val tc = if (game.timeLeft <= 10) AppColors.errorRed else AppColors.gold
                    Text("${game.timeLeft}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = tc, fontFamily = FontFamily.Monospace)
                } else {
                    Text("∞", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = AppColors.gold)
                }
            }
            Spacer(Modifier.height(4.dp))

            val interactAlpha = if (isRoundOver) 0.4f else 1f

            // Expression display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.bgInput)
                    .border(2.dp, when {
                        player.showSuccess -> AppColors.successGreen
                        player.feedback != null && !player.passed -> AppColors.errorRed
                        else -> AppColors.borderDim
                    }, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .alpha(interactAlpha),
                contentAlignment = Alignment.CenterStart
            ) {
                if (player.tokens.isEmpty()) {
                    Text("点击数字和运算符组成算式", fontSize = 14.sp, color = Color(0xFF2B2B2B))
                } else {
                    Text(
                        buildAnnotatedString {
                            player.tokens.forEach { token ->
                                val (color, weight) = when (token) {
                                    is Token.Number -> AppColors.tokenNum to FontWeight.Bold
                                    is Token.Op -> AppColors.tokenOp to FontWeight.Normal
                                    is Token.Paren -> AppColors.tokenParen to FontWeight.Normal
                                }
                                withStyle(SpanStyle(color = color, fontWeight = weight, fontFamily = FontFamily.Monospace)) {
                                    append(token.display)
                                }
                            }
                        },
                        fontSize = 26.sp,
                        maxLines = 1
                    )
                }
            }

            // Feedback
            Text(
                player.feedback ?: "",
                fontSize = 13.sp,
                color = AppColors.errorRed,
                modifier = Modifier.height(20.dp).alpha(if (player.feedback != null) 1f else 0f)
            )

            Spacer(Modifier.height(4.dp))

            // Number buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.alpha(interactAlpha)
            ) {
                for (i in 0 until 4) {
                    val used = player.usedCards.getOrElse(i) { false }
                    val canInput = game.canInputNumber(playerIndex)
                    val disabled = used || !canInput
                    val suit = game.suits.getOrElse(i) { com.twentyfourduel.model.CardSuit.SPADE }
                    val num = game.numbers.getOrElse(i) { 0 }
                    Box(
                        modifier = Modifier
                            .width(70.dp).height(88.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(listOf(Color(28, 34, 64), Color(20, 26, 46))))
                            .border(2.dp, accent.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .alpha(if (used) 0.18f else if (disabled) 0.35f else 1f)
                            .clickable(enabled = !disabled && !isRoundOver) {
                                game.handleNumber(playerIndex, i)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(suit.symbol, fontSize = 10.sp, color = Color.White.copy(alpha = 0.45f))
                            Text("$num", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // Operator buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.alpha(interactAlpha)
            ) {
                val canOp = game.canInputOp(playerIndex)
                val canOpen = game.canInputOpenParen(playerIndex)
                val canClose = game.canInputCloseParen(playerIndex)
                listOf("+" to "+", "-" to "−", "*" to "×", "/" to "÷", "(" to "(", ")" to ")").forEach { (op, label) ->
                    val enabled = when (op) {
                        "(" -> canOpen; ")" -> canClose; else -> canOp
                    }
                    Box(
                        modifier = Modifier
                            .width(50.dp).height(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AppColors.bgCard)
                            .border(2.dp, AppColors.borderDim, RoundedCornerShape(10.dp))
                            .alpha(if (enabled) 1f else 0.3f)
                            .clickable(enabled = enabled && !isRoundOver) {
                                game.handleOp(playerIndex, op)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, fontSize = 21.sp, color = Color(152, 152, 216))
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.alpha(interactAlpha)
            ) {
                ActionBtn("提交", AppColors.successGreen, 88.dp, isRoundOver) { game.handleSubmit(playerIndex) }
                ActionBtn("清空", Color(120, 120, 120), 70.dp, isRoundOver) { game.handleClear(playerIndex) }
                ActionBtn("⌫", Color(120, 120, 120), 70.dp, isRoundOver) { game.handleBackspace(playerIndex) }
            }

            Spacer(Modifier.height(4.dp))
        }

        // Round over overlay
        if (isRoundOver) {
            Box(
                modifier = Modifier.fillMaxSize().background(AppColors.bgMain.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val winner = game.roundWinner
                    if (winner != null) {
                        if (winner == playerIndex) {
                            Text("正确！+1 分", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = AppColors.successGreen)
                        } else {
                            Text("对手先答出", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = AppColors.errorRed)
                            game.solutions.firstOrNull()?.let {
                                Text(it, fontSize = 15.sp, color = AppColors.textDim)
                            }
                        }
                    } else {
                        Text("本轮结束", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = AppColors.gold)
                        game.solutions.firstOrNull()?.let {
                            Text("答案: $it", fontSize = 15.sp, color = AppColors.textDim)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EndGameButton(wantsToEnd: Boolean, onClick: () -> Unit) {
    val bg = if (wantsToEnd) AppColors.gold.copy(alpha = 0.12f) else AppColors.bgCard
    val border = if (wantsToEnd) AppColors.gold.copy(alpha = 0.4f) else AppColors.borderDim
    val tc = if (wantsToEnd) AppColors.gold else Color(128, 128, 128)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(if (wantsToEnd) "取消结束" else "结束", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = tc)
    }
}

@Composable
private fun SkipButton(game: GameState, playerIndex: Int) {
    val player = game.getPlayer(playerIndex)
    val otherSkip = game.getPlayer(1 - playerIndex).wantsToSkip
    val bg = if (player.wantsToSkip) AppColors.gold.copy(alpha = 0.12f) else AppColors.bgCard
    val border = if (player.wantsToSkip) AppColors.gold.copy(alpha = 0.4f) else AppColors.borderDim
    val tc = if (player.wantsToSkip) AppColors.gold else Color(128, 128, 128)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(8.dp))
            .clickable { game.handleRequestSkip(playerIndex) }
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row {
            Text(if (player.wantsToSkip) "取消跳过" else "跳过", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = tc)
            if (otherSkip && !player.wantsToSkip) {
                Spacer(Modifier.width(3.dp))
                Text("(对方已请求)", fontSize = 10.sp, color = tc)
            }
        }
    }
}

@Composable
private fun ActionBtn(
    title: String,
    color: Color,
    minW: androidx.compose.ui.unit.Dp,
    disabled: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (title == "提交") color.copy(alpha = 0.35f) else AppColors.borderDim
    Box(
        modifier = Modifier
            .widthIn(min = minW).height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(AppColors.bgCard)
            .border(2.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(enabled = !disabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}
