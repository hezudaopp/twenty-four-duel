package com.twentyfourduel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twentyfourduel.model.CardSuit
import com.twentyfourduel.model.GamePhase
import com.twentyfourduel.model.GameState
import kotlinx.coroutines.delay

@Composable
fun GameScreen(game: GameState) {
    LaunchedEffect(game.phase) {
        while (true) {
            delay(1000)
            game.tick()
        }
    }

    // Auto-advance after round over
    val phase = game.phase
    LaunchedEffect(phase) {
        if (phase == GamePhase.ROUND_OVER) {
            delay(2500)
            game.advanceAfterRound()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(AppColors.bgMain)
    ) {
        when (game.phase) {
            GamePhase.WELCOME -> WelcomeView(game)
            GamePhase.HISTORY -> HistoryScreen(game)
            GamePhase.PLAYING, GamePhase.ROUND_OVER -> GamePlayView(game)
            GamePhase.GAME_OVER -> ResultView(game)
        }
    }
}

@Composable
private fun GamePlayView(game: GameState) {
    val centerHeight = 60.dp
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .rotate(180f)
        ) {
            PlayerView(game, 1, Modifier.fillMaxSize())
        }

        CenterStrip(game, Modifier.fillMaxWidth().height(centerHeight))

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            PlayerView(game, 0, Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun CenterStrip(game: GameState, modifier: Modifier) {
    Row(
        modifier = modifier
            .background(
                Brush.horizontalGradient(
                    listOf(AppColors.p1Accent.copy(alpha = 0.03f), AppColors.p2Accent.copy(alpha = 0.03f))
                )
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in game.numbers.indices) {
            if (i > 0) Spacer(Modifier.width(7.dp))
            MiniCard(
                game.numbers.getOrElse(i) { 0 },
                game.suits.getOrElse(i) { CardSuit.SPADE }
            )
        }
    }
}

@Composable
private fun MiniCard(number: Int, suit: CardSuit) {
    val textColor = if (suit.isRed) Color(204, 0, 0) else Color(33, 33, 33)
    Box(
        modifier = Modifier
            .width(40.dp).height(54.dp)
            .shadow(3.dp, RoundedCornerShape(5.dp))
            .clip(RoundedCornerShape(5.dp))
            .background(Brush.linearGradient(listOf(AppColors.cardGradLight, AppColors.cardGradDark))),
        contentAlignment = Alignment.Center
    ) {
        Text(suit.symbol, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textColor)
        Text(
            "$number",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.TopStart).padding(start = 4.dp, top = 2.dp)
        )
        Text(
            "$number",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 4.dp, bottom = 2.dp).rotate(180f)
        )
    }
}
