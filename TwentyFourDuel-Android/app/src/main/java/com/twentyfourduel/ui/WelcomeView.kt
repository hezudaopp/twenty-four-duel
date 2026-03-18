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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twentyfourduel.model.Difficulty
import com.twentyfourduel.model.GamePhase
import com.twentyfourduel.model.GameState

@Composable
fun WelcomeView(game: GameState) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "♠ ♥ ♦ ♣",
            fontSize = 28.sp,
            color = AppColors.borderDim,
            letterSpacing = 16.sp
        )
        Spacer(Modifier.height(20.dp))
        Text(
            "24 点对战",
            style = TextStyle(
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                brush = Brush.linearGradient(listOf(AppColors.p1Accent, AppColors.p2Accent))
            )
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "双人竞速 · 谁先算出 24",
            fontSize = 17.sp,
            color = AppColors.textDim,
            letterSpacing = 2.sp
        )
        Spacer(Modifier.height(36.dp))

        SettingRow("难度") {
            Difficulty.entries.forEach { diff ->
                OptionButton(diff.label, game.difficulty == diff) { game.difficulty = diff }
            }
        }
        Spacer(Modifier.height(14.dp))
        SettingRow("局数") {
            listOf(5, 10, 15).forEach { n ->
                OptionButton("$n 局", game.totalRounds == n) { game.totalRounds = n }
            }
        }
        Spacer(Modifier.height(14.dp))
        SettingRow("限时") {
            listOf(30, 60, 90, 0).forEach { t ->
                OptionButton(if (t == 0) "不限" else "$t 秒", game.timeLimit == t) { game.timeLimit = t }
            }
        }
        Spacer(Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.horizontalGradient(listOf(AppColors.p1Accent, AppColors.p1Accent.copy(alpha = 0.7f))))
                .clickable { game.startGame() }
                .padding(horizontal = 60.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("开 始", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = AppColors.bgMain, letterSpacing = 6.sp)
        }

        Spacer(Modifier.height(20.dp))
        Text(
            "⏱ 交战记录",
            fontSize = 15.sp,
            color = AppColors.textDim,
            modifier = Modifier.clickable { game.phase = GamePhase.HISTORY }
        )
    }
}

@Composable
private fun SettingRow(label: String, content: @Composable RowScope.() -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 15.sp, color = AppColors.textDim, modifier = Modifier.width(50.dp), textAlign = TextAlign.End)
        Spacer(Modifier.width(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

@Composable
private fun OptionButton(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) AppColors.p1Accent.copy(alpha = 0.07f) else AppColors.bgCard
    val border = if (selected) AppColors.p1Accent else AppColors.borderDim
    val textColor = if (selected) AppColors.p1Accent else AppColors.textDim
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(2.dp, border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 14.sp, color = textColor)
    }
}
