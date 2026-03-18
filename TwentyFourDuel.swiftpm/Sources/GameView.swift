import SwiftUI

struct GameView: View {
    @StateObject private var game = GameState()

    private let ticker = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    var body: some View {
        ZStack {
            Color.bgMain

            switch game.phase {
            case .welcome:
                WelcomeView(game: game)
                    .transition(.opacity)
            case .history:
                HistoryView {
                    withAnimation(.easeInOut(duration: 0.2)) { game.phase = .welcome }
                }
                .transition(.opacity)
            case .playing, .roundOver:
                gamePlayView
                    .transition(.opacity)
            case .gameOver:
                ResultView(game: game)
                    .transition(.opacity)
            }
        }
        .ignoresSafeArea()
        .onReceive(ticker) { _ in game.tick() }
    }

    private let centerHeight: CGFloat = 60

    private var gamePlayView: some View {
        GeometryReader { geo in
            let playerH = (geo.size.height - centerHeight) / 2
            VStack(spacing: 0) {
                PlayerView(game: game, playerIndex: 1)
                    .rotationEffect(.degrees(180))
                    .frame(width: geo.size.width, height: playerH)

                centerStrip
                    .frame(width: geo.size.width, height: centerHeight)

                PlayerView(game: game, playerIndex: 0)
                    .frame(width: geo.size.width, height: playerH)
            }
        }
        .ignoresSafeArea()
    }

    // MARK: - Center Strip

    private var centerStrip: some View {
        HStack(spacing: 12) {
            centerCards
        }
        .frame(maxWidth: .infinity)
        .background(
            LinearGradient(
                colors: [Color.p1Accent.opacity(0.03), Color.p2Accent.opacity(0.03)],
                startPoint: .leading,
                endPoint: .trailing
            )
        )
        .overlay(alignment: .top) {
            Rectangle().fill(Color.white.opacity(0.04)).frame(height: 1)
        }
        .overlay(alignment: .bottom) {
            Rectangle().fill(Color.white.opacity(0.04)).frame(height: 1)
        }
    }

    private var centerCards: some View {
        HStack(spacing: 7) {
            ForEach(0..<game.numbers.count, id: \.self) { i in
                MiniCardView(
                    number: game.numbers[safe: i] ?? 0,
                    suit: game.suits[safe: i] ?? .spade
                )
            }
        }
    }
}

// MARK: - Mini Card (center decorative)

struct MiniCardView: View {
    let number: Int
    let suit: CardSuit

    private var textColor: Color { suit.isRed ? Color(red: 0.8, green: 0, blue: 0) : Color(white: 0.13) }

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 5)
                .fill(
                    LinearGradient(
                        colors: [.cardGradientLight, .cardGradientDark],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )

            Text(suit.symbol)
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(textColor)

            VStack {
                HStack {
                    Text("\(number)")
                        .font(.system(size: 10, weight: .bold))
                        .foregroundColor(textColor)
                        .padding(.leading, 4).padding(.top, 2)
                    Spacer()
                }
                Spacer()
                HStack {
                    Spacer()
                    Text("\(number)")
                        .font(.system(size: 10, weight: .bold))
                        .foregroundColor(textColor)
                        .rotationEffect(.degrees(180))
                        .padding(.trailing, 4).padding(.bottom, 2)
                }
            }
        }
        .frame(width: 40, height: 54)
        .shadow(color: .black.opacity(0.3), radius: 3, y: 2)
    }
}
