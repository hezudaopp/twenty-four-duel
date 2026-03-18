import SwiftUI

struct WelcomeView: View {
    @ObservedObject var game: GameState

    var body: some View {
        VStack(spacing: 0) {
            Spacer()

            Text("♠ ♥ ♦ ♣")
                .font(.system(size: 28))
                .tracking(16)
                .foregroundColor(.borderDim)
                .padding(.bottom, 20)

            Text("24 点对战")
                .font(.system(size: 52, weight: .black))
                .foregroundStyle(
                    LinearGradient(
                        colors: [.p1Accent, .p2Accent],
                        startPoint: .leading,
                        endPoint: .trailing
                    )
                )
                .padding(.bottom, 4)

            Text("双人竞速 · 谁先算出 24")
                .font(.system(size: 17))
                .foregroundColor(.textDim)
                .tracking(2)
                .padding(.bottom, 36)

            VStack(spacing: 14) {
                settingRow("难度") {
                    ForEach(Difficulty.allCases, id: \.self) { diff in
                        optionButton(diff.label, selected: game.difficulty == diff) {
                            game.difficulty = diff
                        }
                    }
                }
                settingRow("局数") {
                    ForEach([5, 10, 15], id: \.self) { n in
                        optionButton("\(n) 局", selected: game.totalRounds == n) {
                            game.totalRounds = n
                        }
                    }
                }
                settingRow("限时") {
                    ForEach([30, 60, 90, 0], id: \.self) { t in
                        optionButton(t == 0 ? "不限" : "\(t) 秒", selected: game.timeLimit == t) {
                            game.timeLimit = t
                        }
                    }
                }
            }
            .padding(.bottom, 32)

            Button(action: {
                withAnimation(.easeInOut(duration: 0.3)) { game.startGame() }
            }) {
                Text("开 始")
                    .font(.system(size: 22, weight: .heavy))
                    .tracking(6)
                    .foregroundColor(.white)
                    .padding(.horizontal, 60)
                    .padding(.vertical, 16)
                    .background(
                        LinearGradient(
                            colors: [.p1Accent, Color(red: 0, green: 0.47, blue: 0.71)],
                            startPoint: .leading,
                            endPoint: .trailing
                        )
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                    .shadow(color: .p1Accent.opacity(0.25), radius: 12, y: 4)
            }

            Button(action: {
                withAnimation(.easeInOut(duration: 0.2)) { game.phase = .history }
            }) {
                HStack(spacing: 6) {
                    Image(systemName: "clock.arrow.circlepath")
                    Text("交战记录")
                }
                .font(.system(size: 15))
                .foregroundColor(.textDim)
                .padding(.top, 20)
            }

            Spacer()
        }
    }

    // MARK: - Helpers

    private func settingRow<Content: View>(
        _ label: String,
        @ViewBuilder content: () -> Content
    ) -> some View {
        HStack(spacing: 12) {
            Text(label)
                .font(.system(size: 15))
                .foregroundColor(.textDim)
                .frame(width: 50, alignment: .trailing)
            HStack(spacing: 8) { content() }
        }
    }

    private func optionButton(
        _ text: String,
        selected: Bool,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            Text(text)
                .font(.system(size: 14))
                .foregroundColor(selected ? .p1Accent : .textDim)
                .padding(.horizontal, 18)
                .padding(.vertical, 8)
                .background(
                    RoundedRectangle(cornerRadius: 10)
                        .fill(selected ? Color.p1Accent.opacity(0.07) : Color.bgCard)
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 10)
                        .stroke(selected ? Color.p1Accent : Color.borderDim, lineWidth: 2)
                )
        }
    }
}
