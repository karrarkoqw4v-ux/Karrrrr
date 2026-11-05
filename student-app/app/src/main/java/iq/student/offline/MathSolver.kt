package iq.student.offline

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * حلّال رياضيات محلي (بدون ذكاء اصطناعي): تعبيرات حسابية، معادلات خطية، ومعادلات تربيعية، مع شرح الخطوات.
 * يفهم: + - * / ^ وأقواس والضرب الضمني مثل 2x و3(x+1).
 */
object MathSolver {
    data class Result(val steps: List<String>, val answer: String)

    private class Poly(val c: DoubleArray = DoubleArray(5)) {
        operator fun plus(o: Poly) = Poly(DoubleArray(5) { c[it] + o.c[it] })
        operator fun minus(o: Poly) = Poly(DoubleArray(5) { c[it] - o.c[it] })
        operator fun times(o: Poly): Poly {
            val r = DoubleArray(5)
            for (i in 0..4) for (j in 0..4) if (c[i] != 0.0 && o.c[j] != 0.0) {
                if (i + j > 4) error("الدرجة أعلى من 4")
                r[i + j] += c[i] * o.c[j]
            }
            return Poly(r)
        }
        fun degree(): Int { for (i in 4 downTo 0) if (abs(c[i]) > 1e-12) return i; return 0 }
        fun isConst() = degree() == 0
    }

    private class Parser(val s: String, val v: Char) {
        var i = 0
        fun peek() = if (i < s.length) s[i] else '\u0000'
        fun parse(): Poly { val p = expr(); if (i < s.length) error("رمز غير مفهوم: ${s[i]}"); return p }
        fun expr(): Poly {
            var r = term()
            while (peek() == '+' || peek() == '-') { val op = s[i++]; val t = term(); r = if (op == '+') r + t else r - t }
            return r
        }
        fun term(): Poly {
            var r = unary()
            while (true) {
                val ch = peek()
                r = when {
                    ch == '*' -> { i++; r * unary() }
                    ch == '/' -> { i++; val d = unary(); if (!d.isConst() || abs(d.c[0]) < 1e-12) error("قسمة غير مسموحة"); Poly(DoubleArray(5) { r.c[it] / d.c[0] }) }
                    ch == '(' || ch == v || ch.isDigit() || ch == '.' -> r * unary()
                    else -> return r
                }
            }
        }
        fun unary(): Poly {
            if (peek() == '-') { i++; return Poly() - unary() }
            if (peek() == '+') { i++; return unary() }
            return power()
        }
        fun power(): Poly {
            var b = atom()
            if (peek() == '^') {
                i++
                val e = unary()
                if (!e.isConst()) error("الأس لازم رقم")
                val n = e.c[0].toInt()
                if (n < 0 || n > 4) error("الأس خارج النطاق")
                var r = Poly().also { it.c[0] = 1.0 }
                repeat(n) { r = r * b }
                b = r
            }
            return b
        }
        fun atom(): Poly {
            val ch = peek()
            if (ch == '(') { i++; val r = expr(); if (peek() != ')') error("قوس ناقص"); i++; return r }
            if (ch == v) { i++; return Poly().also { it.c[1] = 1.0 } }
            if (ch.isDigit() || ch == '.') {
                val st = i
                while (peek().isDigit() || peek() == '.') i++
                return Poly().also { it.c[0] = s.substring(st, i).toDouble() }
            }
            error("عبارة غير مفهومة")
        }
    }

    private fun normalize(raw: String): String {
        val arabicDigits = "٠١٢٣٤٥٦٧٨٩"
        return raw.map { ch -> val d = arabicDigits.indexOf(ch); if (d >= 0) ('0' + d) else ch }.joinToString("")
            .replace("×", "*").replace("÷", "/").replace("−", "-").replace("–", "-").replace("،", ".").replace(",", ".")
            .replace("²", "^2").replace("³", "^3").replace(" ", "").lowercase()
    }

    fun fmt(x: Double): String {
        if (abs(x - x.toLong()) < 1e-9) return x.toLong().toString()
        // كسر بسيط إن أمكن
        for (d in 2..12) { val n = x * d; if (abs(n - n.toLong()) < 1e-9) return "${n.toLong()}/$d" }
        return String.format(java.util.Locale.US, "%.4f", x).trimEnd('0').trimEnd('.')
    }

    private fun term(c: Double, p: String): String = when {
        p.isEmpty() -> fmt(c)
        abs(c - 1) < 1e-12 -> p
        abs(c + 1) < 1e-12 -> "-$p"
        else -> fmt(c) + p
    }

    private fun show(p: Poly, v: Char): String {
        val parts = mutableListOf<String>()
        for (d in 4 downTo 0) {
            val c = p.c[d]; if (abs(c) < 1e-12) continue
            val name = when (d) { 0 -> ""; 1 -> "$v"; else -> "$v^$d" }
            parts += term(c, name)
        }
        if (parts.isEmpty()) return "0"
        return parts.joinToString(" + ").replace("+ -", "- ")
    }

    fun solve(input: String): Result {
        val s = normalize(input)
        require(s.isNotEmpty()) { "اكتب المسألة أول" }
        val v = s.firstOrNull { it.isLetter() } ?: 'x'
        val eq = s.split("=")
        if (eq.size == 1) {
            val p = Parser(s, v).parse()
            require(p.isConst()) { "هذا تعبير بمتغير؛ اكتبه كمعادلة مثل 2x+6=14" }
            return Result(listOf("نحسب التعبير بالترتيب: الأقواس ثم الأسس ثم الضرب والقسمة ثم الجمع والطرح"), "الناتج = ${fmt(p.c[0])}")
        }
        require(eq.size == 2) { "المعادلة فيها أكثر من علامة =" }
        val left = Parser(eq[0], v).parse()
        val right = Parser(eq[1], v).parse()
        val p = left - right
        val steps = mutableListOf<String>()
        steps += "المعادلة: ${show(left, v)} = ${show(right, v)}"
        steps += "ننقل كل الحدود لطرف واحد: ${show(p, v)} = 0"
        return when (p.degree()) {
            0 -> if (abs(p.c[0]) < 1e-12) Result(steps, "المعادلة صحيحة لكل قيمة (عدد لا نهائي من الحلول)") else Result(steps, "لا يوجد حل")
            1 -> {
                val a = p.c[1]; val b = p.c[0]
                steps += "نعزل $v: ${term(a, "$v")} = ${fmt(-b)}"
                steps += "نقسم الطرفين على ${fmt(a)}: $v = ${fmt(-b)} ÷ ${fmt(a)}"
                val x = -b / a
                steps += "نتحقق بالتعويض: ${fmt(left.c[0] + left.c[1] * x)} = ${fmt(right.c[0] + right.c[1] * x)}"
                Result(steps, "$v = ${fmt(x)}")
            }
            2 -> {
                val a = p.c[2]; val b = p.c[1]; val c0 = p.c[0]
                steps += "معادلة تربيعية على الشكل a$v² + b$v + c = 0 حيث a=${fmt(a)} ، b=${fmt(b)} ، c=${fmt(c0)}"
                val d = b * b - 4 * a * c0
                steps += "المميّز Δ = b² - 4ac = ${fmt(b * b)} - ${fmt(4 * a * c0)} = ${fmt(d)}"
                when {
                    d < -1e-12 -> Result(steps + "Δ سالب، فلا توجد جذور حقيقية", "لا يوجد حل حقيقي")
                    abs(d) < 1e-12 -> {
                        val x = -b / (2 * a)
                        Result(steps + "Δ = 0 فيوجد جذر واحد مكرر: $v = -b ÷ 2a", "$v = ${fmt(x)}")
                    }
                    else -> {
                        val r = sqrt(d)
                        val x1 = (-b + r) / (2 * a); val x2 = (-b - r) / (2 * a)
                        steps += "نطبق القانون: $v = (-b ± √Δ) ÷ 2a"
                        steps += "√Δ = ${fmt(r)}"
                        val int1 = abs(x1 - Math.rint(x1)) < 1e-9; val int2 = abs(x2 - Math.rint(x2)) < 1e-9
                        if (int1 && int2) steps += "التحليل: (${v} - ${fmt(x1)})(${v} - ${fmt(x2)}) = 0".replace("- -", "+ ")
                        Result(steps, "$v = ${fmt(x1)}   أو   $v = ${fmt(x2)}")
                    }
                }
            }
            else -> Result(steps, "الحلّال يدعم معادلات حتى الدرجة الثانية فقط")
        }
    }

    /** يحاول يرجّع نتيجة أو رسالة خطأ مفهومة */
    fun safe(input: String): Result = try { solve(input) } catch (e: IllegalArgumentException) {
        Result(emptyList(), e.message ?: "تعذر حل المسألة")
    } catch (e: IllegalStateException) {
        Result(emptyList(), e.message ?: "تعذر قراءة المسألة")
    } catch (e: NumberFormatException) {
        Result(emptyList(), "رقم غير مفهوم")
    }
}
