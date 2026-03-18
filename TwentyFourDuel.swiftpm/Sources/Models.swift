import SwiftUI

// MARK: - Game Phase

enum GamePhase: Equatable {
    case welcome
    case history
    case playing
    case roundOver
    case gameOver
}

// MARK: - Difficulty

enum Difficulty: String, CaseIterable {
    case easy, normal

    var label: String {
        switch self {
        case .easy: return "简单 1-9"
        case .normal: return "普通 1-13"
        }
    }

    var maxNumber: Int {
        switch self {
        case .easy: return 9
        case .normal: return 13
        }
    }
}

// MARK: - Card Suit

enum CardSuit: Int, CaseIterable {
    case spade, heart, diamond, club

    var symbol: String {
        switch self {
        case .spade: return "♠"
        case .heart: return "♥"
        case .diamond: return "♦"
        case .club: return "♣"
        }
    }

    var isRed: Bool { self == .heart || self == .diamond }

    static var random: CardSuit { allCases.randomElement()! }
}

// MARK: - Token

enum Token: Equatable {
    case number(value: Int, cardIndex: Int)
    case op(String)
    case paren(String)

    var display: String {
        switch self {
        case .number(let v, _): return "\(v)"
        case .op(let o):
            switch o {
            case "*": return "×"
            case "/": return "÷"
            case "-": return "−"
            default: return o
            }
        case .paren(let p): return p
        }
    }

    var evalString: String {
        switch self {
        case .number(let v, _): return "\(v)"
        case .op(let o): return o
        case .paren(let p): return p
        }
    }

    var isNumber: Bool {
        if case .number = self { return true }
        return false
    }
}

// MARK: - Player State

struct PlayerState {
    var score: Int = 0
    var tokens: [Token] = []
    var usedCards: [Bool] = [false, false, false, false]
    var passed: Bool = false
    var feedback: String? = nil
    var shakeCount: Int = 0
    var showSuccess: Bool = false
    var wantsToEnd: Bool = false
    var wantsToSkip: Bool = false
}

// MARK: - Game History

struct RoundRecord: Codable, Identifiable {
    var id = UUID()
    let roundNumber: Int
    let numbers: [Int]
    let player1Expression: String
    let player2Expression: String
    let player1Result: RoundResult
    let player2Result: RoundResult
    let winnerIndex: Int?
    let solutions: [String]
}

enum RoundResult: String, Codable {
    case correct
    case wrong
    case timeout
    case skipped
    case noAnswer
}

struct GameRecord: Codable, Identifiable {
    var id = UUID()
    let date: Date
    let difficulty: String
    let totalRounds: Int
    let timeLimit: Int
    let player1Score: Int
    let player2Score: Int
    let rounds: [RoundRecord]

    var winnerText: String {
        if player1Score > player2Score { return "玩家一胜" }
        if player2Score > player1Score { return "玩家二胜" }
        return "平局"
    }
}

final class HistoryStore {
    static let shared = HistoryStore()
    private let key = "game_history_v1"

    private init() {}

    func load() -> [GameRecord] {
        guard let data = UserDefaults.standard.data(forKey: key),
              let records = try? JSONDecoder().decode([GameRecord].self, from: data)
        else { return [] }
        return records.sorted { $0.date > $1.date }
    }

    func save(_ record: GameRecord) {
        var records = load()
        records.insert(record, at: 0)
        if records.count > 50 { records = Array(records.prefix(50)) }
        if let data = try? JSONEncoder().encode(records) {
            UserDefaults.standard.set(data, forKey: key)
        }
    }

    func delete(id: UUID) {
        var records = load()
        records.removeAll { $0.id == id }
        if let data = try? JSONEncoder().encode(records) {
            UserDefaults.standard.set(data, forKey: key)
        }
    }
}

// MARK: - Theme Colors

extension Color {
    static let bgMain = Color(red: 10/255, green: 14/255, blue: 26/255)
    static let bgCard = Color(red: 17/255, green: 22/255, blue: 34/255)
    static let bgInput = Color(red: 12/255, green: 16/255, blue: 24/255)
    static let borderDim = Color(red: 30/255, green: 36/255, blue: 64/255)

    static let p1Accent = Color(red: 0, green: 210/255, blue: 1)
    static let p2Accent = Color(red: 1, green: 107/255, blue: 107/255)
    static let gold = Color(red: 1, green: 215/255, blue: 0)
    static let successGreen = Color(red: 0, green: 1, blue: 136/255)
    static let errorRed = Color(red: 1, green: 68/255, blue: 68/255)
    static let tokenNum = Color(red: 1, green: 215/255, blue: 0)
    static let tokenOp = Color(red: 184/255, green: 184/255, blue: 1)
    static let tokenParen = Color(white: 0.35)
    static let textDim = Color(white: 0.4)
    static let cardGradientLight = Color(red: 248/255, green: 245/255, blue: 238/255)
    static let cardGradientDark = Color(red: 229/255, green: 224/255, blue: 212/255)
}

// MARK: - Shake Effect

struct ShakeEffect: GeometryEffect {
    var amount: CGFloat = 5
    var shakes = 3
    var animatableData: CGFloat

    func effectValue(size: CGSize) -> ProjectionTransform {
        let translation = amount * sin(animatableData * .pi * CGFloat(shakes))
        return ProjectionTransform(CGAffineTransform(translationX: translation, y: 0))
    }
}

// MARK: - Array Safe Subscript

extension Array {
    subscript(safe index: Int) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}
