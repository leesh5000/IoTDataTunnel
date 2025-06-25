package me.helloc.iot.tunnel

/**
 * Minimal JSON parser supporting objects, arrays, strings, numbers, booleans and null.
 * It converts JSON into Kotlin types: Map for objects, List for arrays, String, Int/Double,
 * Boolean and null.
 */
internal class SimpleJsonParser(private val json: String) {
    private var index = 0
    private val length = json.length

    fun parse(): Any? {
        skipWhitespace()
        val value = parseValue()
        skipWhitespace()
        return value
    }

    private fun parseValue(): Any? {
        skipWhitespace()
        if (index >= length) return null
        return when (json[index]) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> parseString()
            't' -> { consumeLiteral("true"); true }
            'f' -> { consumeLiteral("false"); false }
            'n' -> { consumeLiteral("null"); null }
            else -> parseNumber()
        }
    }

    private fun parseObject(): Map<String, Any?> {
        expect('{')
        skipWhitespace()
        val map = mutableMapOf<String, Any?>()
        if (peek() == '}') {
            index++
            return map
        }
        while (index < length) {
            skipWhitespace()
            val key = parseString()
            skipWhitespace()
            expect(':')
            skipWhitespace()
            val value = parseValue()
            map[key] = value
            skipWhitespace()
            when (peek()) {
                ',' -> { index++; continue }
                '}' -> { index++; return map }
                else -> error("Unexpected character at position $index")
            }
        }
        error("Unterminated object")
    }

    private fun parseArray(): List<Any?> {
        expect('[')
        skipWhitespace()
        val list = mutableListOf<Any?>()
        if (peek() == ']') {
            index++
            return list
        }
        while (index < length) {
            val value = parseValue()
            list.add(value)
            skipWhitespace()
            when (peek()) {
                ',' -> { index++; continue }
                ']' -> { index++; return list }
                else -> error("Unexpected character at position $index")
            }
        }
        error("Unterminated array")
    }

    private fun parseString(): String {
        expect('"')
        val sb = StringBuilder()
        while (index < length) {
            val ch = json[index++]
            when (ch) {
                '\\' -> {
                    if (index >= length) error("Incomplete escape sequence")
                    val esc = json[index++]
                    sb.append(
                        when (esc) {
                            '"' -> '"'
                            '\\' -> '\\'
                            '/' -> '/'
                            'b' -> '\b'
                            'f' -> '\u000C'
                            'n' -> '\n'
                            'r' -> '\r'
                            't' -> '\t'
                            'u' -> {
                                val hex = json.substring(index, index + 4)
                                index += 4
                                hex.toInt(16).toChar()
                            }
                            else -> esc
                        }
                    )
                }
                '"' -> return sb.toString()
                else -> sb.append(ch)
            }
        }
        error("Unterminated string")
    }

    private fun parseNumber(): Any {
        val start = index
        if (json[index] == '-') index++
        while (index < length && json[index].isDigit()) index++
        if (index < length && json[index] == '.') {
            index++
            while (index < length && json[index].isDigit()) index++
        }
        if (index < length && (json[index] == 'e' || json[index] == 'E')) {
            index++
            if (index < length && (json[index] == '+' || json[index] == '-')) index++
            while (index < length && json[index].isDigit()) index++
        }
        val numberStr = json.substring(start, index)
        return numberStr.toLongOrNull() ?: numberStr.toDouble()
    }

    private fun skipWhitespace() {
        while (index < length && json[index].isWhitespace()) index++
    }

    private fun expect(ch: Char) {
        if (index >= length || json[index] != ch) error("Expected '$ch' at position $index")
        index++
    }

    private fun consumeLiteral(lit: String) {
        for (c in lit) {
            if (index >= length || json[index] != c) error("Expected literal $lit at position $index")
            index++
        }
    }

    private fun peek(): Char? = if (index < length) json[index] else null
}
