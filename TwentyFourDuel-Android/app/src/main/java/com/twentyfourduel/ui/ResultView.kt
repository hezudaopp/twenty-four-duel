package com.twentyfourduel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twentyfourduel.model.GameState

@Composable
fun ResultView(game: GameState) {
    val s0 = game.player0.score
    val s1 = game.player1.score
    val winText = when {
        s0 > s1 -> "玩家一 获胜！"
        s1 > s0 -> "玩家二 获胜！"
        else -> "平局！旗鼓相当"
    }
    val winColor = when {
        s0 > s1 -> AppColors.p1Accent
        s1 > s0 -> AppColors.p2Accent
        else -> AppColors.gold
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "游戏结束",
            style = TextStyle(
                fontSize = 44.sp,
                fontWeight = FontWeight.Black,
                brush = Brush.linearGradient(listOf(AppColors.gold, AppColors.p2Accent))
            )
        )
        Spacer(Modifier.height(6.dp))
        Text(winText, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = winColor)
        Spacer(Modifier.height(28.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
            ScoreCol("玩家一", s0, AppColors.p1Accent)
            ScoreCol("玩家二", s1, AppColors.p2Accent)
        }
        Spacer(Modifier.height(36.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(AppColors.gold.copy(alpha = 0.06f))
                .border(2.dp, AppColors.gold, RoundedCornerShape(14.dp))
                .clickable { game.backToWelcome() }
                .padding(horizontal = 44.dp, vertical = 14.dp)
        ) {
            Text("再来一局", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColors.gold)
        }
    }
}

@Composable
private fun ScoreCol(name: String, score: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(name, fontSize = 16.sp, color = AppColors.textDim)
        Spacer(Modifier.height(6.dp))
        Text("$score", fontSize = 48.sp, fontWeight = FontWeight.Black, color = color)
    }
}
