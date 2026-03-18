import Foundation

// MARK: - Expression Evaluator (Recursive Descent Parser)

struct ExpressionEvaluator {
    private var chars: [Character]
    private var pos: Int

    static func evaluate(_ string: String) -> Double? {
        var evaluator = ExpressionEvaluator(string)
        let result = evaluator.parseExpression()
        return evaluator.pos == evaluator.chars.count ? result : nil
    }

    private init(_ string: String) {
        self.chars = Array(string)
        self.pos = 0
    }

    private mutating func parseExpression() -> Double? {
        guard var result = parseTerm() else { return nil }
        while pos < chars.count && (chars[pos] == "+" || chars[pos] == "-") {
            let op = chars[pos]; pos += 1
            guard let right = parseTerm() else { return nil }
            result = op == "+" ? result + right : result - right
        }
        return result
    }

    private mutating func parseTerm() -> Double? {
        guard var result = parseFactor() else { return nil }
        while pos < chars.count && (chars[pos] == "*" || chars[pos] == "/") {
            let op = chars[pos]; pos += 1
            guard let right = parseFactor() else { return nil }
            result = op == "*" ? result * right : result / right
        }
        return result
    }

    private mutating func parseFactor() -> Double? {
        if pos < chars.count && chars[pos] == "(" {
            pos += 1
            let result = parseExpression()
            guard pos < chars.count && chars[pos] == ")" else { return nil }
            pos += 1
            return result
        }
        var numStr = ""
        while pos < chars.count && chars[pos].isNumber {
            numStr.append(chars[pos]); pos += 1
        }
        return numStr.isEmpty ? nil : Double(numStr)
    }
}

// MARK: - 24-Point Solver

struct Solver {
    static func solve24(_ numbers: [Int]) -> [String] {
        var results = Set<String>()
        let ops = ["+", "-", "*", "/"]

        for perm in permutations(numbers) {
            let a = perm[0], b = perm[1], c = perm[2], d = perm[3]
            for o1 in ops {
                for o2 in ops {
                    for o3 in ops {
                        let patterns = [
                            "((\(a)\(o1)\(b))\(o2)\(c))\(o3)\(d)",
                            "(\(a)\(o1)(\(b)\(o2)\(c)))\(o3)\(d)",
                            "(\(a)\(o1)\(b))\(o2)(\(c)\(o3)\(d))",
                            "\(a)\(o1)((\(b)\(o2)\(c))\(o3)\(d))",
                            "\(a)\(o1)(\(b)\(o2)(\(c)\(o3)\(d)))",
                        ]
                        for expr in patterns {
                            if let val = ExpressionEvaluator.evaluate(expr),
                               abs(val - 24) < 1e-9 {
                                let display = expr
                                    .replacingOccurrences(of: "*", with: "×")
                                    .replacingOccurrences(of: "/", with: "÷")
                                results.insert(display)
                            }
                        }
                    }
                }
            }
        }
        return Array(results)
    }

    private static func permutations(_ array: [Int]) -> [[Int]] {
        if array.count <= 1 { return [array] }
        var result: [[Int]] = []
        for i in array.indices {
            var rest = array
            rest.remove(at: i)
            for perm in permutations(rest) {
                result.append([array[i]] + perm)
            }
        }
        return result
    }
}
