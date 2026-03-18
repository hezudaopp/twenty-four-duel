import SwiftUI

struct ResultView: View {
    @ObservedObject var game: GameState

    private var s0: Int { game.players[0].score }
    private var s1: Int { game.players[1].score }

    private var winnerText: String {
        if s0 > s1 { return "玩家一 获胜！" }
        if s1 > s0 { return "玩家二 获胜！" }
        return "平局！旗鼓相当"
    }

    private var winnerColor: Color {
        if s0 > s1 { return .p1Accent }
        if s1 > s0 { return .p2Accent }
        return .gold
    }

    var body: some View {
        VStack(spacing: 0) {
            Spacer()

            Text("游戏结束")
                .font(.system(size: 48, weight: .black))
                .foregroundStyle(
                    LinearGradient(
                        colors: [.gold, .p2Accent],
                        startPoint: .leading,
                        endPoint: .trailing
                    )
                )
                .padding(.bottom, 6)

            Text(winnerText)
                .font(.system(size: 22, weight: .bold))
                .foregroundColor(winnerColor)
                .padding(.bottom, 28)

            HStack(spacing: 40) {
                scoreColumn(name: "玩家一", score: s0, color: .p1Accent)
                scoreColumn(name: "玩家二", score: s1, color: .p2Accent)
            }
            .padding(.bottom, 36)

            Button {
                withAnimation(.easeInOut(duration: 0.3)) { game.backToWelcome() }
            } label: {
                Text("再来一局")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(.gold)
                    .padding(.horizontal, 44)
                    .padding(.vertical, 14)
                    .background(
                        RoundedRectangle(cornerRadius: 14)
                            .fill(Color.gold.opacity(0.06))
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: 14)
                            .stroke(Color.gold, lineWidth: 2)
                    )
            }

            Spacer()
        }
    }

    private func scoreColumn(name: String, score: Int, color: Color) -> some View {
        VStack(spacing: 6) {
            Text(name)
                .font(.system(size: 16))
                .foregroundColor(.textDim)
            Text("\(score)")
                .font(.system(size: 52, weight: .black))
                .foregroundColor(color)
        }
    }
}
