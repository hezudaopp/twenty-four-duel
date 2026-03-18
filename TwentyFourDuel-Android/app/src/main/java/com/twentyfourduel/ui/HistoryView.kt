package com.twentyfourduel.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twentyfourduel.model.*
import java.text.SimpleDateFormat
import java.util.*

enum class HistoryTab(val label: String) { GAMES("交战记录"), UNSOLVED("错题集") }

@Composable
fun HistoryScreen(game: GameState) {
    val store = game.historyStore ?: return
    var records by remember { mutableStateOf(store.load()) }
    var selectedRecord by remember { mutableStateOf<GameRecord?>(null) }
    var selectedTab by remember { mutableStateOf(HistoryTab.GAMES) }

    val unsolvedRounds = remember(records) {
        records.flatMap { rec ->
            rec.rounds.filter { it.player1Result != RoundResult.CORRECT && it.player2Result != RoundResult.CORRECT }
                .map { rec.date to it }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(AppColors.bgMain)) {
        if (selectedRecord != null) {
            GameDetailScreen(selectedRecord!!) { selectedRecord = null }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Nav bar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("← 返回", fontSize = 15.sp, color = AppColors.p1Accent,
                        modifier = Modifier.clickable { game.phase = GamePhase.WELCOME })
                    Spacer(Modifier.weight(1f))
                    Text(selectedTab.label, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.weight(1f))
                    Spacer(Modifier.width(50.dp))
                }

                // Tab bar
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp)) {
                    HistoryTab.entries.forEach { tab ->
                        Column(
                            modifier = Modifier.weight(1f).clickable { selectedTab = tab },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    tab.label,
                                    fontSize = 14.sp,
                                    fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == tab) Color.White else AppColors.textDim
                                )
                                if (tab == HistoryTab.UNSOLVED) {
                                    Spacer(Modifier.width(4.dp))
                                    Box(
                                        Modifier.clip(RoundedCornerShape(50))
                                            .background(if (selectedTab == tab) AppColors.gold else AppColors.borderDim)
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            "${unsolvedRounds.size}", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                            color = if (selectedTab == tab) AppColors.bgMain else AppColors.textDim
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Box(
                                Modifier.fillMaxWidth().height(2.dp)
                                    .background(if (selectedTab == tab) AppColors.p1Accent else Color.Transparent)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                when (selectedTab) {
                    HistoryTab.GAMES -> {
                        if (records.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("暂无交战记录", fontSize = 17.sp, color = AppColors.textDim)
                                    Text("完成一场游戏后记录将出现在这里", fontSize = 14.sp, color = Color(64, 64, 64))
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(records, key = { it.id }) { record ->
                                    RecordRow(record,
                                        onClick = { selectedRecord = record },
                                        onDelete = {
                                            store.delete(record.id)
                                            records = store.load()
                                        }
                                    )
                                }
                            }
                        }
                    }
                    HistoryTab.UNSOLVED -> {
                        if (unsolvedRounds.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("暂无错题", fontSize = 17.sp, color = AppColors.textDim)
                                    Text("双方都未答对的题目会出现在这里", fontSize = 14.sp, color = Color(64, 64, 64))
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(unsolvedRounds.size) { idx ->
                                    val (date, round) = unsolvedRounds[idx]
                                    UnsolvedCard(round, date)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecordRow(record: GameRecord, onClick: () -> Unit, onDelete: () -> Unit) {
    val winColor = when {
        record.player1Score > record.player2Score -> AppColors.p1Accent
        record.player2Score > record.player1Score -> AppColors.p2Accent
        else -> AppColors.gold
    }
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.bgCard)
            .border(1.dp, AppColors.borderDim, RoundedCornerShape(14.dp))
            .combinedClickable(onClick = onClick, onLongClick = onDelete)
            .padding(14.dp)
    ) {
        Row {
            Text(formatDate(record.date), fontSize = 12.sp, color = AppColors.textDim)
            Spacer(Modifier.weight(1f))
            Text(record.difficulty, fontSize = 12.sp, color = AppColors.textDim)
            Spacer(Modifier.width(8.dp))
            Text(if (record.timeLimit > 0) "${record.timeLimit}秒" else "不限时", fontSize = 12.sp, color = AppColors.textDim)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("玩家一", fontSize = 13.sp, color = AppColors.p1Accent)
                Text("${record.player1Score}", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = AppColors.p1Accent)
            }
            Spacer(Modifier.width(16.dp))
            Text(":", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AppColors.textDim)
            Spacer(Modifier.width(16.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("玩家二", fontSize = 13.sp, color = AppColors.p2Accent)
                Text("${record.player2Score}", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = AppColors.p2Accent)
            }
            Spacer(Modifier.weight(1f))
            Text(record.winnerText, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = winColor)
        }
        Spacer(Modifier.height(8.dp))
        Row {
            Text("共 ${record.rounds.size} 轮", fontSize = 12.sp, color = AppColors.textDim)
            Spacer(Modifier.weight(1f))
            Text("查看详情 ›", fontSize = 12.sp, color = AppColors.textDim)
        }
    }
}

@Composable
private fun UnsolvedCard(round: RoundRecord, date: Long) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.bgCard)
            .border(1.dp, AppColors.borderDim, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                round.numbers.forEach { n ->
                    Box(
                        Modifier.size(36.dp, 34.dp).clip(RoundedCornerShape(8.dp)).background(AppColors.bgMain),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("$n", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, color = Color.White)
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Text(formatDate(date), fontSize = 11.sp, color = Color(64, 64, 64))
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(AppColors.borderDim))
        Spacer(Modifier.height(8.dp))
        PlayerResultRow("玩家一", round.player1Expression, round.player1Result, AppColors.p1Accent)
        Spacer(Modifier.height(4.dp))
        PlayerResultRow("玩家二", round.player2Expression, round.player2Result, AppColors.p2Accent)
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(AppColors.borderDim))
        Spacer(Modifier.height(8.dp))
        Row {
            Text("答案:", fontSize = 12.sp, color = AppColors.textDim)
            Spacer(Modifier.width(4.dp))
            Text(
                round.solutions.take(2).joinToString("  "),
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace, color = AppColors.gold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PlayerResultRow(name: String, expr: String, result: RoundResult, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = accent, modifier = Modifier.width(46.dp))
        Text(
            expr.ifEmpty { "未作答" },
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            color = if (expr.isEmpty()) Color(64, 64, 64) else Color.White,
            maxLines = 1
        )
        Spacer(Modifier.weight(1f))
        ResultBadge(result)
    }
}

@Composable
private fun ResultBadge(result: RoundResult) {
    val (text, color) = when (result) {
        RoundResult.CORRECT -> "正确" to AppColors.successGreen
        RoundResult.WRONG -> "错误" to AppColors.errorRed
        RoundResult.TIMEOUT -> "超时" to Color(102, 102, 102)
        RoundResult.SKIPPED -> "跳过" to Color(102, 102, 102)
        RoundResult.NO_ANSWER -> "未答" to Color(77, 77, 77)
    }
    Box(
        Modifier.clip(RoundedCornerShape(5.dp)).background(color.copy(alpha = 0.12f)).padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}

@Composable
private fun GameDetailScreen(record: GameRecord, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(AppColors.bgMain)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("← 返回", fontSize = 15.sp, color = AppColors.p1Accent, modifier = Modifier.clickable(onClick = onBack))
            Spacer(Modifier.weight(1f))
            Text("对战详情", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(50.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("玩家一", fontSize = 13.sp, color = AppColors.p1Accent)
                Text("${record.player1Score}", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = AppColors.p1Accent)
            }
            Spacer(Modifier.width(20.dp))
            Text("VS", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppColors.textDim)
            Spacer(Modifier.width(20.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("玩家二", fontSize = 13.sp, color = AppColors.p2Accent)
                Text("${record.player2Score}", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = AppColors.p2Accent)
            }
        }

        Spacer(Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(record.rounds, key = { it.id }) { round ->
                RoundCard(round)
            }
        }
    }
}

@Composable
private fun RoundCard(round: RoundRecord) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.bgCard)
            .border(1.dp, AppColors.borderDim, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("第 ${round.roundNumber} 轮", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                round.numbers.forEach { n ->
                    Box(
                        Modifier.size(30.dp, 28.dp).clip(RoundedCornerShape(6.dp)).background(AppColors.bgMain),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("$n", fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = AppColors.gold)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(AppColors.borderDim))
        Spacer(Modifier.height(8.dp))
        DetailPlayerRow("玩家一", round.player1Expression, round.player1Result, round.winnerIndex == 0, AppColors.p1Accent)
        Spacer(Modifier.height(4.dp))
        DetailPlayerRow("玩家二", round.player2Expression, round.player2Result, round.winnerIndex == 1, AppColors.p2Accent)
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(AppColors.borderDim))
        Spacer(Modifier.height(8.dp))
        Row {
            Text("答案:", fontSize = 12.sp, color = AppColors.textDim)
            Spacer(Modifier.width(4.dp))
            Text(
                round.solutions.take(2).joinToString("  "),
                fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = AppColors.gold, maxLines = 1
            )
        }
    }
}

@Composable
private fun DetailPlayerRow(name: String, expr: String, result: RoundResult, isWinner: Boolean, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = accent, modifier = Modifier.width(50.dp))
        Text(
            expr.ifEmpty { "未作答" },
            fontSize = 14.sp, fontFamily = FontFamily.Monospace,
            color = if (expr.isEmpty()) Color(64, 64, 64) else Color.White, maxLines = 1
        )
        Spacer(Modifier.weight(1f))
        ResultBadge(result)
    }
}

private fun formatDate(timestamp: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
