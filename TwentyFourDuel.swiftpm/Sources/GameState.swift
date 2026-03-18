import SwiftUI
import AVFoundation

@MainActor
class GameState: ObservableObject {
    private let sound = SoundManager.shared
    @Published var phase: GamePhase = .welcome
    @Published var difficulty: Difficulty = .normal
    @Published var totalRounds: Int = 10
    @Published var timeLimit: Int = 60
    @Published var currentRound: Int = 0
    @Published var numbers: [Int] = []
    @Published var suits: [CardSuit] = []
    @Published var solutions: [String] = []
    @Published var timeLeft: Int = 60
    @Published var players: [PlayerState] = [PlayerState(), PlayerState()]
    @Published var roundWinner: Int? = nil

    private var roundRecords: [RoundRecord] = []
    private var playerExpressions: [String] = ["", ""]
    private var playerResults: [RoundResult] = [.noAnswer, .noAnswer]

    // MARK: - Game Lifecycle

    func startGame() {
        currentRound = 0
        players = [PlayerState(), PlayerState()]
        roundRecords = []
        UIApplication.shared.isIdleTimerDisabled = true
        startRound()
    }

    func startRound() {
        currentRound += 1
        generateNumbers()
        for i in 0..<2 {
            players[i].tokens = []
            players[i].usedCards = [false, false, false, false]
            players[i].passed = false
            players[i].feedback = nil
            players[i].showSuccess = false
            players[i].wantsToEnd = false
            players[i].wantsToSkip = false
        }
        roundWinner = nil
        playerExpressions = ["", ""]
        playerResults = [.noAnswer, .noAnswer]
        phase = .playing
        timeLeft = timeLimit
    }

    func tick() {
        guard phase == .playing, timeLimit > 0 else { return }
        timeLeft -= 1
        if timeLeft <= 0 {
            sound.play("timeout")
            endRound(winner: nil)
        } else if timeLeft <= 10 {
            sound.play("tick")
        }
    }

    func endRound(winner: Int?) {
        guard phase == .playing else { return }
        phase = .roundOver
        roundWinner = winner
        if let w = winner { players[w].score += 1 }

        if winner == nil {
            let isSkip = players[0].wantsToSkip && players[1].wantsToSkip
            let isTimeout = timeLimit > 0 && timeLeft <= 0
            for i in 0..<2 {
                if playerExpressions[i].isEmpty {
                    playerExpressions[i] = players[i].tokens.map(\.display).joined()
                }
                if playerResults[i] == .noAnswer {
                    playerResults[i] = isSkip ? .skipped : (isTimeout ? .timeout : .noAnswer)
                }
            }
        }

        let record = RoundRecord(
            roundNumber: currentRound,
            numbers: numbers,
            player1Expression: playerExpressions[0],
            player2Expression: playerExpressions[1],
            player1Result: playerResults[0],
            player2Result: playerResults[1],
            winnerIndex: winner,
            solutions: Array(solutions.prefix(3))
        )
        roundRecords.append(record)

        DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) { [weak self] in
            guard let self, self.phase == .roundOver else { return }
            if self.currentRound >= self.totalRounds {
                self.endGame()
            } else {
                self.startRound()
            }
        }
    }

    func endGame() {
        phase = .gameOver
        sound.play("gameover")
        UIApplication.shared.isIdleTimerDisabled = false
        saveGameRecord()
    }

    private func recordCurrentRoundIfNeeded() {
        let alreadyRecorded = roundRecords.contains { $0.roundNumber == currentRound }
        guard !alreadyRecorded, !numbers.isEmpty else { return }
        for i in 0..<2 {
            if playerExpressions[i].isEmpty {
                playerExpressions[i] = players[i].tokens.map(\.display).joined()
            }
            if playerResults[i] == .noAnswer {
                playerResults[i] = .noAnswer
            }
        }
        let record = RoundRecord(
            roundNumber: currentRound,
            numbers: numbers,
            player1Expression: playerExpressions[0],
            player2Expression: playerExpressions[1],
            player1Result: playerResults[0],
            player2Result: playerResults[1],
            winnerIndex: nil,
            solutions: Array(solutions.prefix(3))
        )
        roundRecords.append(record)
    }

    private func saveGameRecord() {
        let gameRecord = GameRecord(
            date: Date(),
            difficulty: difficulty.label,
            totalRounds: totalRounds,
            timeLimit: timeLimit,
            player1Score: players[0].score,
            player2Score: players[1].score,
            rounds: roundRecords
        )
        HistoryStore.shared.save(gameRecord)
    }

    func backToWelcome() {
        phase = .welcome
        UIApplication.shared.isIdleTimerDisabled = false
    }

    // MARK: - Input Validation

    func canInputNumber(player: Int) -> Bool {
        guard let last = players[player].tokens.last else { return true }
        switch last {
        case .number: return false
        case .op: return true
        case .paren(let p): return p == "("
        }
    }

    func canInputOp(player: Int) -> Bool {
        guard let last = players[player].tokens.last else { return false }
        switch last {
        case .number: return true
        case .op: return false
        case .paren(let p): return p == ")"
        }
    }

    func canInputOpenParen(player: Int) -> Bool {
        let tokens = players[player].tokens
        let openCount = tokens.filter { if case .paren("(") = $0 { return true }; return false }.count
        guard openCount < 3 else { return false }
        guard let last = tokens.last else { return true }
        switch last {
        case .number: return false
        case .op: return true
        case .paren(let p): return p == "("
        }
    }

    func canInputCloseParen(player: Int) -> Bool {
        let tokens = players[player].tokens
        let openCount = tokens.filter { if case .paren("(") = $0 { return true }; return false }.count
        let closeCount = tokens.filter { if case .paren(")") = $0 { return true }; return false }.count
        guard openCount > closeCount else { return false }
        guard let last = tokens.last else { return false }
        switch last {
        case .number: return true
        case .op: return false
        case .paren(let p): return p == ")"
        }
    }

    // MARK: - Player Actions

    func handleNumber(player: Int, cardIndex: Int) {
        guard phase == .playing,
              !players[player].usedCards[cardIndex],
              canInputNumber(player: player) else { return }
        players[player].passed = false
        players[player].tokens.append(.number(value: numbers[cardIndex], cardIndex: cardIndex))
        players[player].usedCards[cardIndex] = true
        players[player].feedback = nil
        sound.play("tap")
        haptic(.light)
    }

    func handleOp(player: Int, op: String) {
        guard phase == .playing else { return }
        if op == "(" {
            guard canInputOpenParen(player: player) else { return }
        } else if op == ")" {
            guard canInputCloseParen(player: player) else { return }
        } else {
            guard canInputOp(player: player) else { return }
        }
        players[player].passed = false
        let token: Token = (op == "(" || op == ")") ? .paren(op) : .op(op)
        players[player].tokens.append(token)
        players[player].feedback = nil
        sound.play("tap")
        haptic(.light)
    }

    func handleBackspace(player: Int) {
        guard phase == .playing, !players[player].tokens.isEmpty else { return }
        players[player].passed = false
        let removed = players[player].tokens.removeLast()
        if case .number(_, let idx) = removed {
            players[player].usedCards[idx] = false
        }
        players[player].feedback = nil
        haptic(.light)
    }

    func handleClear(player: Int) {
        guard phase == .playing else { return }
        players[player].passed = false
        players[player].tokens = []
        players[player].usedCards = [false, false, false, false]
        players[player].feedback = nil
        haptic(.light)
    }

    func handleSubmit(player: Int) {
        guard phase == .playing else { return }
        let p = players[player]

        let numCount = p.tokens.filter(\.isNumber).count
        guard numCount == 4, !p.usedCards.contains(false) else {
            sound.play("wrong")
            showError(player: player, text: "请使用全部 4 个数字")
            return
        }

        let exprStr = p.tokens.map(\.evalString).joined()

        guard let val = ExpressionEvaluator.evaluate(exprStr), val.isFinite else {
            sound.play("wrong")
            showError(player: player, text: "算式格式错误")
            return
        }

        let displayExpr = p.tokens.map(\.display).joined()
        playerExpressions[player] = displayExpr

        if abs(val - 24) < 1e-9 {
            playerResults[player] = .correct
            players[player].showSuccess = true
            sound.play("correct")
            haptic(.success)
            endRound(winner: player)
        } else {
            playerResults[player] = .wrong
            let display = val == val.rounded() && abs(val) < 1e6
                ? String(Int(val))
                : String(format: "%.2f", val)
            sound.play("wrong")
            showError(player: player, text: "= \(display)，不等于 24")
        }
    }

    func handlePass(player: Int) {
        guard phase == .playing else { return }
        players[player].passed = true
        players[player].feedback = "已放弃，等待对手..."
        if players[0].passed && players[1].passed {
            endRound(winner: nil)
        }
    }

    func handleRequestSkip(player: Int) {
        guard phase == .playing, timeLimit == 0 else { return }
        players[player].wantsToSkip.toggle()
        if players[player].wantsToSkip {
            players[player].feedback = "已请求跳过，等待对方同意..."
            haptic(.light)
        } else {
            players[player].feedback = nil
        }
        if players[0].wantsToSkip && players[1].wantsToSkip {
            haptic(.success)
            endRound(winner: nil)
        }
    }

    func handleRequestEnd(player: Int) {
        guard phase == .playing || phase == .roundOver else { return }
        players[player].wantsToEnd.toggle()
        if players[player].wantsToEnd {
            haptic(.light)
        }
        if players[0].wantsToEnd && players[1].wantsToEnd {
            haptic(.success)
            recordCurrentRoundIfNeeded()
            saveGameRecord()
            backToWelcome()
        }
    }

    // MARK: - Private Helpers

    private func showError(player: Int, text: String) {
        players[player].feedback = text
        players[player].shakeCount += 1
        haptic(.error)
    }

    private func generateNumbers() {
        let max = difficulty.maxNumber
        for _ in 0..<500 {
            let nums = (0..<4).map { _ in Int.random(in: 1...max) }
            let sols = Solver.solve24(nums)
            if !sols.isEmpty {
                numbers = nums
                suits = (0..<4).map { _ in CardSuit.random }
                solutions = sols
                return
            }
        }
        numbers = [1, 2, 3, 4]
        suits = [.spade, .heart, .diamond, .club]
        solutions = Solver.solve24(numbers)
    }

    private enum HapticType { case light, success, error }

    private func haptic(_ type: HapticType) {
        switch type {
        case .light:
            UIImpactFeedbackGenerator(style: .light).impactOccurred()
        case .success:
            UINotificationFeedbackGenerator().notificationOccurred(.success)
        case .error:
            UINotificationFeedbackGenerator().notificationOccurred(.error)
        }
    }
}
