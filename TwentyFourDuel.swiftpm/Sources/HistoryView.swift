import SwiftUI

private struct SwipeBackModifier: ViewModifier {
    let action: () -> Void
    @State private var offset: CGFloat = 0

    func body(content: Content) -> some View {
        content
            .offset(x: offset)
            .gesture(
                DragGesture(minimumDistance: 20, coordinateSpace: .global)
                    .onChanged { value in
                        if value.startLocation.x < 40 && value.translation.width > 0 {
                            offset = value.translation.width
                        }
                    }
                    .onEnded { value in
                        if value.startLocation.x < 40 && value.translation.width > 80 {
                            withAnimation(.easeInOut(duration: 0.2)) {
                                offset = UIScreen.main.bounds.width
                            }
                            DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
                                offset = 0
                                action()
                            }
                        } else {
                            withAnimation(.easeOut(duration: 0.15)) { offset = 0 }
                        }
                    }
            )
    }
}

private extension View {
    func swipeBack(_ action: @escaping () -> Void) -> some View {
        modifier(SwipeBackModifier(action: action))
    }
}

enum HistoryTab: String, CaseIterable {
    case games = "交战记录"
    case unsolved = "错题集"
}

struct HistoryView: View {
    let onBack: () -> Void
    @State private var records: [GameRecord] = []
    @State private var selectedRecord: GameRecord? = nil
    @State private var selectedTab: HistoryTab = .games

    private var unsolvedRounds: [(date: Date, round: RoundRecord)] {
        records.flatMap { record in
            record.rounds
                .filter { $0.player1Result != .correct && $0.player2Result != .correct }
                .map { (date: record.date, round: $0) }
        }
    }

    var body: some View {
        ZStack {
            Color.bgMain.ignoresSafeArea()

            if let record = selectedRecord {
                GameDetailView(record: record) {
                    withAnimation(.easeInOut(duration: 0.2)) { selectedRecord = nil }
                }
                .swipeBack {
                    withAnimation(.easeInOut(duration: 0.2)) { selectedRecord = nil }
                }
                .transition(.move(edge: .trailing))
            } else {
                mainList
                    .swipeBack(onBack)
                    .transition(.move(edge: .leading))
            }
        }
        .onAppear { records = HistoryStore.shared.load() }
    }

    // MARK: - Main List with Tabs

    private var mainList: some View {
        VStack(spacing: 0) {
            HStack {
                Button(action: onBack) {
                    HStack(spacing: 4) {
                        Image(systemName: "chevron.left")
                        Text("返回")
                    }
                    .font(.system(size: 15))
                    .foregroundColor(.p1Accent)
                }
                Spacer()
                Text(selectedTab.rawValue)
                    .font(.system(size: 20, weight: .bold))
                Spacer()
                Text("").frame(width: 50)
            }
            .padding(.horizontal, 20)
            .padding(.top, 16)
            .padding(.bottom, 8)

            tabBar
                .padding(.bottom, 10)

            if selectedTab == .games {
                gamesContent
            } else {
                unsolvedContent
            }
        }
    }

    private var tabBar: some View {
        HStack(spacing: 0) {
            ForEach(HistoryTab.allCases, id: \.self) { tab in
                Button {
                    withAnimation(.easeInOut(duration: 0.15)) { selectedTab = tab }
                } label: {
                    VStack(spacing: 6) {
                        HStack(spacing: 4) {
                            Text(tab.rawValue)
                                .font(.system(size: 14, weight: selectedTab == tab ? .bold : .regular))
                            if tab == .unsolved {
                                Text("\(unsolvedRounds.count)")
                                    .font(.system(size: 11, weight: .bold))
                                    .foregroundColor(selectedTab == tab ? .bgMain : .textDim)
                                    .padding(.horizontal, 5)
                                    .padding(.vertical, 1)
                                    .background(
                                        Capsule().fill(selectedTab == tab ? Color.gold : Color.borderDim)
                                    )
                            }
                        }
                        .foregroundColor(selectedTab == tab ? .white : .textDim)

                        Rectangle()
                            .fill(selectedTab == tab ? Color.p1Accent : Color.clear)
                            .frame(height: 2)
                    }
                }
                .frame(maxWidth: .infinity)
            }
        }
        .padding(.horizontal, 40)
    }

    // MARK: - Games Tab

    private var gamesContent: some View {
        Group {
            if records.isEmpty {
                VStack {
                    Spacer()
                    Text("暂无交战记录")
                        .font(.system(size: 17))
                        .foregroundColor(.textDim)
                    Text("完成一场游戏后记录将出现在这里")
                        .font(.system(size: 14))
                        .foregroundColor(Color(white: 0.25))
                        .padding(.top, 4)
                    Spacer()
                }
            } else {
                ScrollView {
                    LazyVStack(spacing: 10) {
                        ForEach(records) { record in
                            recordRow(record)
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.bottom, 20)
                }
            }
        }
    }

    // MARK: - Unsolved Tab

    private var unsolvedContent: some View {
        Group {
            if unsolvedRounds.isEmpty {
                VStack {
                    Spacer()
                    Text("暂无错题")
                        .font(.system(size: 17))
                        .foregroundColor(.textDim)
                    Text("双方都未答对的题目会出现在这里")
                        .font(.system(size: 14))
                        .foregroundColor(Color(white: 0.25))
                        .padding(.top, 4)
                    Spacer()
                }
            } else {
                ScrollView {
                    LazyVStack(spacing: 10) {
                        ForEach(Array(unsolvedRounds.enumerated()), id: \.offset) { _, item in
                            unsolvedCard(item.round, date: item.date)
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.bottom, 20)
                }
            }
        }
    }

    private func unsolvedCard(_ round: RoundRecord, date: Date) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                HStack(spacing: 8) {
                    ForEach(round.numbers, id: \.self) { n in
                        Text("\(n)")
                            .font(.system(size: 18, weight: .heavy, design: .monospaced))
                            .foregroundColor(.white)
                            .frame(width: 36, height: 34)
                            .background(RoundedRectangle(cornerRadius: 8).fill(Color.bgMain))
                    }
                }
                Spacer()
                Text(formatDate(date))
                    .font(.system(size: 11))
                    .foregroundColor(Color(white: 0.25))
            }

            Divider().overlay(Color.borderDim)

            HStack(spacing: 8) {
                Text("玩家一")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(.p1Accent)
                    .frame(width: 46, alignment: .leading)
                Text(round.player1Expression.isEmpty ? "未作答" : round.player1Expression)
                    .font(.system(size: 13, design: .monospaced))
                    .foregroundColor(round.player1Expression.isEmpty ? Color(white: 0.25) : .white)
                    .lineLimit(1)
                Spacer()
                resultLabel(round.player1Result)
            }

            HStack(spacing: 8) {
                Text("玩家二")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(.p2Accent)
                    .frame(width: 46, alignment: .leading)
                Text(round.player2Expression.isEmpty ? "未作答" : round.player2Expression)
                    .font(.system(size: 13, design: .monospaced))
                    .foregroundColor(round.player2Expression.isEmpty ? Color(white: 0.25) : .white)
                    .lineLimit(1)
                Spacer()
                resultLabel(round.player2Result)
            }

            Divider().overlay(Color.borderDim)

            HStack(spacing: 4) {
                Text("答案:")
                    .font(.system(size: 12))
                    .foregroundColor(.textDim)
                Text(round.solutions.prefix(2).joined(separator: "  "))
                    .font(.system(size: 13, weight: .semibold, design: .monospaced))
                    .foregroundColor(.gold)
                    .lineLimit(1)
            }
        }
        .padding(12)
        .background(RoundedRectangle(cornerRadius: 12).fill(Color.bgCard))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.borderDim, lineWidth: 1))
    }

    private func resultLabel(_ result: RoundResult) -> some View {
        let (text, color): (String, Color) = {
            switch result {
            case .correct: return ("正确", .successGreen)
            case .wrong: return ("错误", .errorRed)
            case .timeout: return ("超时", Color(white: 0.4))
            case .skipped: return ("跳过", Color(white: 0.4))
            case .noAnswer: return ("未答", Color(white: 0.3))
            }
        }()
        return Text(text)
            .font(.system(size: 11, weight: .semibold))
            .foregroundColor(color)
            .padding(.horizontal, 6)
            .padding(.vertical, 2)
            .background(RoundedRectangle(cornerRadius: 5).fill(color.opacity(0.12)))
    }

    private func recordRow(_ record: GameRecord) -> some View {
        VStack(spacing: 8) {
            HStack {
                Text(formatDate(record.date))
                    .font(.system(size: 12))
                    .foregroundColor(.textDim)
                Spacer()
                Text(record.difficulty)
                    .font(.system(size: 12))
                    .foregroundColor(.textDim)
                if record.timeLimit > 0 {
                    Text("\(record.timeLimit)秒")
                        .font(.system(size: 12))
                        .foregroundColor(.textDim)
                } else {
                    Text("不限时")
                        .font(.system(size: 12))
                        .foregroundColor(.textDim)
                }
            }

            HStack(spacing: 16) {
                VStack(spacing: 2) {
                    Text("玩家一")
                        .font(.system(size: 13))
                        .foregroundColor(.p1Accent)
                    Text("\(record.player1Score)")
                        .font(.system(size: 28, weight: .heavy))
                        .foregroundColor(.p1Accent)
                }

                Text(":")
                    .font(.system(size: 24, weight: .bold))
                    .foregroundColor(.textDim)

                VStack(spacing: 2) {
                    Text("玩家二")
                        .font(.system(size: 13))
                        .foregroundColor(.p2Accent)
                    Text("\(record.player2Score)")
                        .font(.system(size: 28, weight: .heavy))
                        .foregroundColor(.p2Accent)
                }

                Spacer()

                Text(record.winnerText)
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(winnerColor(record))
            }

            HStack {
                Text("共 \(record.rounds.count) 轮")
                    .font(.system(size: 12))
                    .foregroundColor(.textDim)

                Spacer()

                HStack(spacing: 2) {
                    Text("查看详情")
                        .font(.system(size: 12))
                        .foregroundColor(.textDim)
                    Image(systemName: "chevron.right")
                        .font(.system(size: 10))
                        .foregroundColor(.textDim)
                }
            }
        }
        .padding(14)
        .background(
            RoundedRectangle(cornerRadius: 14)
                .fill(Color.bgCard)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 14)
                .stroke(Color.borderDim, lineWidth: 1)
        )
        .onTapGesture {
            withAnimation(.easeInOut(duration: 0.2)) { selectedRecord = record }
        }
        .contextMenu {
            Button(role: .destructive) {
                HistoryStore.shared.delete(id: record.id)
                withAnimation { records.removeAll { $0.id == record.id } }
            } label: {
                Label("删除记录", systemImage: "trash")
            }
        }
    }

    private func winnerColor(_ record: GameRecord) -> Color {
        if record.player1Score > record.player2Score { return .p1Accent }
        if record.player2Score > record.player1Score { return .p2Accent }
        return .gold
    }

    private func formatDate(_ date: Date) -> String {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd HH:mm"
        return f.string(from: date)
    }
}

// MARK: - Game Detail View

struct GameDetailView: View {
    let record: GameRecord
    let onBack: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Button(action: onBack) {
                    HStack(spacing: 4) {
                        Image(systemName: "chevron.left")
                        Text("返回")
                    }
                    .font(.system(size: 15))
                    .foregroundColor(.p1Accent)
                }
                Spacer()
                Text("对战详情")
                    .font(.system(size: 20, weight: .bold))
                Spacer()
                Text("").frame(width: 50)
            }
            .padding(.horizontal, 20)
            .padding(.top, 16)
            .padding(.bottom, 6)

            scoreSummary
                .padding(.bottom, 10)

            ScrollView {
                LazyVStack(spacing: 12) {
                    ForEach(record.rounds) { round in
                        roundCard(round)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 20)
            }
        }
    }

    private var scoreSummary: some View {
        HStack(spacing: 20) {
            VStack(spacing: 2) {
                Text("玩家一").font(.system(size: 13)).foregroundColor(.p1Accent)
                Text("\(record.player1Score)")
                    .font(.system(size: 32, weight: .heavy)).foregroundColor(.p1Accent)
            }
            Text("VS")
                .font(.system(size: 14, weight: .bold))
                .foregroundColor(.textDim)
            VStack(spacing: 2) {
                Text("玩家二").font(.system(size: 13)).foregroundColor(.p2Accent)
                Text("\(record.player2Score)")
                    .font(.system(size: 32, weight: .heavy)).foregroundColor(.p2Accent)
            }
        }
    }

    private func roundCard(_ round: RoundRecord) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("第 \(round.roundNumber) 轮")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(.white)

                Spacer()

                HStack(spacing: 6) {
                    ForEach(round.numbers, id: \.self) { n in
                        Text("\(n)")
                            .font(.system(size: 14, weight: .bold, design: .monospaced))
                            .foregroundColor(.gold)
                            .frame(width: 30, height: 28)
                            .background(RoundedRectangle(cornerRadius: 6).fill(Color.bgMain))
                    }
                }
            }

            Divider().overlay(Color.borderDim)

            playerRow("玩家一", expr: round.player1Expression,
                      result: round.player1Result, isWinner: round.winnerIndex == 0,
                      accent: .p1Accent)

            playerRow("玩家二", expr: round.player2Expression,
                      result: round.player2Result, isWinner: round.winnerIndex == 1,
                      accent: .p2Accent)

            Divider().overlay(Color.borderDim)

            HStack(spacing: 4) {
                Text("答案:")
                    .font(.system(size: 12))
                    .foregroundColor(.textDim)
                Text(round.solutions.prefix(2).joined(separator: "  "))
                    .font(.system(size: 12, design: .monospaced))
                    .foregroundColor(.gold)
                    .lineLimit(1)
            }
        }
        .padding(12)
        .background(RoundedRectangle(cornerRadius: 12).fill(Color.bgCard))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.borderDim, lineWidth: 1))
    }

    private func playerRow(_ name: String, expr: String, result: RoundResult,
                           isWinner: Bool, accent: Color) -> some View {
        HStack(spacing: 8) {
            Text(name)
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(accent)
                .frame(width: 50, alignment: .leading)

            if expr.isEmpty {
                Text("未作答")
                    .font(.system(size: 13))
                    .foregroundColor(Color(white: 0.25))
            } else {
                Text(expr)
                    .font(.system(size: 14, design: .monospaced))
                    .foregroundColor(.white)
                    .lineLimit(1)
            }

            Spacer()

            resultBadge(result, isWinner: isWinner)
        }
    }

    private func resultBadge(_ result: RoundResult, isWinner: Bool) -> some View {
        let (text, color): (String, Color) = {
            switch result {
            case .correct: return ("正确", .successGreen)
            case .wrong: return ("错误", .errorRed)
            case .timeout: return ("超时", Color(white: 0.4))
            case .skipped: return ("跳过", Color(white: 0.4))
            case .noAnswer: return ("未答", Color(white: 0.3))
            }
        }()

        return Text(text)
            .font(.system(size: 11, weight: .semibold))
            .foregroundColor(color)
            .padding(.horizontal, 8)
            .padding(.vertical, 3)
            .background(RoundedRectangle(cornerRadius: 6).fill(color.opacity(0.12)))
    }
}
