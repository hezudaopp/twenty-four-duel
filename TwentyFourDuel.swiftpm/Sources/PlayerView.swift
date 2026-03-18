import SwiftUI

struct PlayerView: View {
    @ObservedObject var game: GameState
    let playerIndex: Int

    private var player: PlayerState { game.players[playerIndex] }
    private var accent: Color { playerIndex == 0 ? .p1Accent : .p2Accent }
    private var name: String { playerIndex == 0 ? "玩家一" : "玩家二" }
    private var isRoundOver: Bool { game.phase == .roundOver }

    var body: some View {
        ZStack {
            playerBackground

            VStack(spacing: 0) {
                Spacer(minLength: 4)
                infoBar
                roundTimerBar
                Spacer(minLength: 4)

                Group {
                    expressionDisplay
                    feedbackText
                    Spacer(minLength: 8)
                    numberButtons
                    Spacer(minLength: 6)
                    operatorButtons
                    Spacer(minLength: 6)
                    actionButtons
                }
                .allowsHitTesting(!isRoundOver)
                .opacity(isRoundOver ? 0.4 : 1)

                Spacer(minLength: 4)
            }
            .padding(.horizontal, 16)

            if isRoundOver { roundOverlay }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .clipped()
    }

    // MARK: - Info Bar

    private var otherWantsToEnd: Bool {
        game.players[1 - playerIndex].wantsToEnd
    }

    private var infoBar: some View {
        HStack(spacing: 6) {
            Circle()
                .fill(accent)
                .frame(width: 10, height: 10)
                .shadow(color: accent, radius: 4)
            Text(name)
                .font(.system(size: 15, weight: .bold))

            endGameButton

            if otherWantsToEnd && !player.wantsToEnd {
                Text("对方请求结束")
                    .font(.system(size: 11))
                    .foregroundColor(.gold)
            }

            Spacer()

            if game.timeLimit == 0 {
                skipButtonInBar
            }

            Text("\(player.score)")
                .font(.system(size: 20, weight: .heavy))
                .foregroundColor(accent)
                .contentTransition(.numericText())
                .animation(.spring(response: 0.3), value: player.score)
        }
        .frame(maxWidth: 560)
        .padding(.horizontal, 4)
    }

    private var endGameButton: some View {
        Button {
            game.handleRequestEnd(player: playerIndex)
        } label: {
            Text(player.wantsToEnd ? "取消结束" : "结束")
                .font(.system(size: 12, weight: .semibold))
                .foregroundColor(player.wantsToEnd ? .gold : Color(white: 0.5))
                .padding(.horizontal, 10)
                .padding(.vertical, 5)
                .background(
                    RoundedRectangle(cornerRadius: 8)
                        .fill(player.wantsToEnd ? Color.gold.opacity(0.12) : Color.bgCard)
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(player.wantsToEnd ? Color.gold.opacity(0.4) : Color.borderDim, lineWidth: 1)
                )
        }
    }

    private var skipButtonInBar: some View {
        Button {
            game.handleRequestSkip(player: playerIndex)
        } label: {
            HStack(spacing: 3) {
                Text(player.wantsToSkip ? "取消跳过" : "跳过")
                    .font(.system(size: 12, weight: .semibold))
                if otherWantsToSkip && !player.wantsToSkip {
                    Text("(对方已请求)")
                        .font(.system(size: 10))
                }
            }
            .foregroundColor(player.wantsToSkip ? .gold : Color(white: 0.5))
            .padding(.horizontal, 10)
            .padding(.vertical, 5)
            .background(
                RoundedRectangle(cornerRadius: 8)
                    .fill(player.wantsToSkip ? Color.gold.opacity(0.12) : Color.bgCard)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 8)
                    .stroke(player.wantsToSkip ? Color.gold.opacity(0.4) : Color.borderDim, lineWidth: 1)
            )
        }
        .padding(.trailing, 4)
    }

    // MARK: - Round & Timer

    private var roundTimerBar: some View {
        HStack(spacing: 12) {
            Text("第 \(game.currentRound) 轮 / 共 \(game.totalRounds) 轮")
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(.textDim)

            if game.timeLimit > 0 {
                HStack(spacing: 4) {
                    Image(systemName: "clock")
                        .font(.system(size: 11))
                        .foregroundColor(.textDim)
                    Text("\(game.timeLeft)")
                        .font(.system(size: 18, weight: .heavy).monospacedDigit())
                        .foregroundColor(game.timeLeft <= 10 ? .errorRed : .gold)
                        .opacity(game.timeLeft <= 10 ? (game.timeLeft % 2 == 0 ? 1 : 0.4) : 1)
                        .animation(.easeInOut(duration: 0.3), value: game.timeLeft)
                }
            } else {
                HStack(spacing: 4) {
                    Image(systemName: "clock")
                        .font(.system(size: 11))
                        .foregroundColor(.textDim)
                    Text("∞")
                        .font(.system(size: 18, weight: .heavy))
                        .foregroundColor(.gold)
                }
            }
        }
        .frame(maxWidth: 560)
        .padding(.vertical, 2)
    }

    // MARK: - Expression Display

    private var expressionDisplay: some View {
        Group {
            if player.tokens.isEmpty {
                Text("点击数字和运算符组成算式")
                    .font(.system(size: 14))
                    .foregroundColor(Color(white: 0.17))
            } else {
                buildExpressionText()
                    .lineLimit(1)
                    .minimumScaleFactor(0.6)
            }
        }
        .frame(maxWidth: 560)
        .frame(minHeight: 46)
        .padding(.horizontal, 14)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color.bgInput)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(expressionBorderColor, lineWidth: 2)
        )
        .modifier(ShakeEffect(animatableData: CGFloat(player.shakeCount)))
        .animation(.easeInOut(duration: 0.35), value: player.shakeCount)
    }

    private var expressionBorderColor: Color {
        if player.showSuccess { return .successGreen }
        if player.feedback != nil && !player.passed { return .errorRed }
        return .borderDim
    }

    private func buildExpressionText() -> Text {
        player.tokens.reduce(Text("")) { result, token in
            result + tokenText(token)
        }
    }

    private func tokenText(_ token: Token) -> Text {
        switch token {
        case .number:
            return Text(token.display)
                .font(.system(size: 26, weight: .bold, design: .monospaced))
                .foregroundColor(.tokenNum)
        case .op:
            return Text(token.display)
                .font(.system(size: 26, design: .monospaced))
                .foregroundColor(.tokenOp)
        case .paren:
            return Text(token.display)
                .font(.system(size: 26, design: .monospaced))
                .foregroundColor(.tokenParen)
        }
    }

    // MARK: - Feedback

    private var feedbackText: some View {
        Text(player.feedback ?? " ")
            .font(.system(size: 13))
            .foregroundColor(.errorRed)
            .opacity(player.feedback != nil ? 1 : 0)
            .frame(height: 17)
            .animation(.easeOut(duration: 0.2), value: player.feedback)
    }

    // MARK: - Number Buttons

    private var numberButtons: some View {
        HStack(spacing: 10) {
            ForEach(0..<4, id: \.self) { i in
                numberCardButton(index: i)
            }
        }
    }

    private func numberCardButton(index: Int) -> some View {
        let used = player.usedCards[safe: index] ?? false
        let canInput = game.canInputNumber(player: playerIndex)
        let disabled = used || !canInput
        let number = game.numbers[safe: index] ?? 0
        let suit = game.suits[safe: index] ?? .spade

        return Button {
            game.handleNumber(player: playerIndex, cardIndex: index)
        } label: {
            ZStack {
                RoundedRectangle(cornerRadius: 12)
                    .fill(
                        LinearGradient(
                            colors: [
                                Color(red: 28/255, green: 34/255, blue: 64/255),
                                Color(red: 20/255, green: 26/255, blue: 46/255)
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                RoundedRectangle(cornerRadius: 12)
                    .stroke(accent.opacity(0.15), lineWidth: 2)

                VStack(spacing: 0) {
                    Text(suit.symbol)
                        .font(.system(size: 10))
                        .opacity(0.45)
                    Text("\(number)")
                        .font(.system(size: 30, weight: .bold))
                        .foregroundColor(.white)
                }
            }
            .frame(width: 76, height: 96)
            .opacity(used ? 0.18 : (disabled ? 0.35 : 1))
            .scaleEffect(used ? 0.9 : 1)
            .animation(.easeOut(duration: 0.15), value: used)
            .animation(.easeOut(duration: 0.1), value: canInput)
        }
        .disabled(disabled)
    }

    // MARK: - Operator Buttons

    private var operatorButtons: some View {
        let canOp = game.canInputOp(player: playerIndex)
        let canOpen = game.canInputOpenParen(player: playerIndex)
        let canClose = game.canInputCloseParen(player: playerIndex)

        return HStack(spacing: 8) {
            ForEach([("+", "+"), ("-", "−"), ("*", "×"), ("/", "÷"), ("(", "("), (")", ")")], id: \.0) { op, label in
                let enabled: Bool = {
                    switch op {
                    case "(": return canOpen
                    case ")": return canClose
                    default: return canOp
                    }
                }()

                Button {
                    game.handleOp(player: playerIndex, op: op)
                } label: {
                    Text(label)
                        .font(.system(size: 23))
                        .foregroundColor(Color(red: 152/255, green: 152/255, blue: 216/255))
                        .frame(width: 54, height: 48)
                        .background(RoundedRectangle(cornerRadius: 10).fill(Color.bgCard))
                        .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color.borderDim, lineWidth: 2))
                        .opacity(enabled ? 1 : 0.3)
                }
                .disabled(!enabled)
            }
        }
        .animation(.easeOut(duration: 0.1), value: canOp)
        .animation(.easeOut(duration: 0.1), value: canOpen)
        .animation(.easeOut(duration: 0.1), value: canClose)
    }

    // MARK: - Action Buttons

    private var otherWantsToSkip: Bool {
        game.players[1 - playerIndex].wantsToSkip
    }

    private var actionButtons: some View {
        HStack(spacing: 7) {
            actionBtn("提交", color: .successGreen, minW: 88) {
                game.handleSubmit(player: playerIndex)
            }
            actionBtn("清空", color: Color(white: 0.47)) {
                game.handleClear(player: playerIndex)
            }
            actionBtn("⌫", color: Color(white: 0.47)) {
                game.handleBackspace(player: playerIndex)
            }
        }
    }

    private func actionBtn(
        _ title: String,
        color: Color,
        minW: CGFloat = 70,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(color)
                .frame(minWidth: minW, minHeight: 48)
                .background(RoundedRectangle(cornerRadius: 10).fill(Color.bgCard))
                .overlay(
                    RoundedRectangle(cornerRadius: 10)
                        .stroke(title == "提交" ? color.opacity(0.35) : Color.borderDim, lineWidth: 2)
                )
        }
    }

    // MARK: - Round Over Overlay

    private var roundOverlay: some View {
        VStack(spacing: 6) {
            if let winner = game.roundWinner {
                if winner == playerIndex {
                    Text("正确！+1 分")
                        .font(.system(size: 30, weight: .heavy))
                        .foregroundColor(.successGreen)
                } else {
                    Text("对手先答出")
                        .font(.system(size: 30, weight: .heavy))
                        .foregroundColor(.errorRed)
                    if let sol = game.solutions.first {
                        Text(sol)
                            .font(.system(size: 15))
                            .foregroundColor(.textDim)
                    }
                }
            } else {
                Text("本轮结束")
                    .font(.system(size: 30, weight: .heavy))
                    .foregroundColor(.gold)
                if let sol = game.solutions.first {
                    Text("答案: \(sol)")
                        .font(.system(size: 15))
                        .foregroundColor(.textDim)
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.bgMain.opacity(0.7))
        .transition(.opacity)
    }

    // MARK: - Background

    private var playerBackground: some View {
        RadialGradient(
            colors: [accent.opacity(0.025), .clear],
            center: .bottom,
            startRadius: 0,
            endRadius: 400
        )
    }
}
