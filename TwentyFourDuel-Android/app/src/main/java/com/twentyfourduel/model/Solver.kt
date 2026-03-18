package com.twentyfourduel.model

import kotlin.math.abs

object ExpressionEvaluator {
    fun evaluate(expression: String): Double? {
        val parser = Parser(expression.toList())
        val result = parser.parseExpression()
        return if (parser.pos == parser.chars.size) result else null
    }

    private class Parser(val chars: List<Char>) {
        var pos = 0

        fun parseExpression(): Double? {
            var result = parseTerm() ?: return null
            while (pos < chars.size && (chars[pos] == '+' || chars[pos] == '-')) {
                val op = chars[pos]; pos++
                val right = parseTerm() ?: return null
                result = if (op == '+') result + right else result - right
            }
            return result
        }

        fun parseTerm(): Double? {
            var result = parseFactor() ?: return null
            while (pos < chars.size && (chars[pos] == '*' || chars[pos] == '/')) {
                val op = chars[pos]; pos++
                val right = parseFactor() ?: return null
                result = if (op == '*') result * right else result / right
            }
            return result
        }

        fun parseFactor(): Double? {
            if (pos < chars.size && chars[pos] == '(') {
                pos++
                val result = parseExpression()
                if (pos >= chars.size || chars[pos] != ')') return null
                pos++
                return result
            }
            val sb = StringBuilder()
            while (pos < chars.size && chars[pos].isDigit()) {
                sb.append(chars[pos]); pos++
            }
            return if (sb.isEmpty()) null else sb.toString().toDoubleOrNull()
        }
    }
}

object Solver {
    fun solve24(numbers: List<Int>): List<String> {
        val results = mutableSetOf<String>()
        val ops = listOf("+", "-", "*", "/")

        for (perm in permutations(numbers)) {
            val (a, b, c, d) = perm
            for (o1 in ops) for (o2 in ops) for (o3 in ops) {
                val patterns = listOf(
                    "(($a$o1$b)$o2$c)$o3$d",
                    "($a$o1($b$o2$c))$o3$d",
                    "($a$o1$b)$o2($c$o3$d)",
                    "$a$o1(($b$o2$c)$o3$d)",
                    "$a$o1($b$o2($c$o3$d))"
                )
                for (expr in patterns) {
                    val v = ExpressionEvaluator.evaluate(expr)
                    if (v != null && v.isFinite() && abs(v - 24.0) < 1e-9) {
                        results.add(
                            expr.replace("*", "×").replace("/", "÷")
                        )
                    }
                }
            }
        }
        return results.toList()
    }

    private fun permutations(list: List<Int>): List<List<Int>> {
        if (list.size <= 1) return listOf(list)
        val result = mutableListOf<List<Int>>()
        for (i in list.indices) {
            val rest = list.toMutableList().also { it.removeAt(i) }
            for (perm in permutations(rest)) {
                result.add(listOf(list[i]) + perm)
            }
        }
        return result
    }
}
