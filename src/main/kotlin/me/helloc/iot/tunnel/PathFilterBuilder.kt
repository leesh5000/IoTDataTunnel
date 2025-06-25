package me.helloc.iot.tunnel


/**
 * Filters JSON messages using simple path expressions and extracts values when all
 * path filters match. Does not rely on external JSON libraries.
 */
class PathFilterBuilder private constructor(private val message: String) {

    private val pathFilters = mutableListOf<Pair<String, Any>>()
    private val valueFilters = mutableListOf<String>()

    fun addPathFilter(path: String, expectedValue: Any): PathFilterBuilder = apply {
        pathFilters.add(path to expectedValue)
    }

    fun addValueFilter(path: String): PathFilterBuilder = apply {
        valueFilters.add(path)
    }

    fun <T> extractFirst(clazz: Class<T>): T? {
        val root = try {
            SimpleJsonParser(message).parse()
        } catch (_: Exception) {
            return null
        }

        for ((path, expected) in pathFilters) {
            val actual = evaluatePath(root, path) ?: return null
            if (actual is Number && expected is Number) {
                if (actual.toDouble() != expected.toDouble()) return null
            } else if (actual != expected) {
                return null
            }
        }

        for (path in valueFilters) {
            val raw = evaluatePath(root, path)
            if (raw != null) {
                val targetClass: Class<*> = when (clazz) {
                    java.lang.Boolean.TYPE -> java.lang.Boolean::class.java
                    java.lang.Byte.TYPE -> java.lang.Byte::class.java
                    java.lang.Character.TYPE -> java.lang.Character::class.java
                    java.lang.Short.TYPE -> java.lang.Short::class.java
                    java.lang.Integer.TYPE -> java.lang.Integer::class.java
                    java.lang.Long.TYPE -> java.lang.Long::class.java
                    java.lang.Float.TYPE -> java.lang.Float::class.java
                    java.lang.Double.TYPE -> java.lang.Double::class.java
                    else -> clazz
                }
                if (targetClass.isInstance(raw)) {
                    @Suppress("UNCHECKED_CAST")
                    return targetClass.cast(raw) as T
                } else if (raw is Number) {
                    val converted: Any? = when (targetClass) {
                        java.lang.Byte::class.java -> raw.toByte()
                        java.lang.Short::class.java -> raw.toShort()
                        java.lang.Integer::class.java -> raw.toInt()
                        java.lang.Long::class.java -> raw.toLong()
                        java.lang.Float::class.java -> raw.toFloat()
                        java.lang.Double::class.java -> raw.toDouble()
                        else -> null
                    }
                    if (converted != null && targetClass.isInstance(converted)) {
                        @Suppress("UNCHECKED_CAST")
                        return converted as T
                    }
                }
            }
        }

        return null
    }

    private fun evaluatePath(root: Any?, path: String): Any? {
        val tokens = parseTokens(path)
        var current: Any? = root
        for (token in tokens) {
            current = when (token) {
                is PathToken.Key -> (current as? Map<*, *>)?.get(token.name)
                is PathToken.Index -> (current as? List<*>)?.getOrNull(token.index)
            } ?: return null
        }
        return current
    }

    private sealed interface PathToken {
        data class Key(val name: String) : PathToken
        data class Index(val index: Int) : PathToken
    }

    private fun parseTokens(path: String): List<PathToken> {
        require(path.startsWith("$")) { "Path must start with $" }
        val tokens = mutableListOf<PathToken>()
        var i = 1
        while (i < path.length) {
            when (path[i]) {
                '.' -> {
                    i++
                    val start = i
                    while (i < path.length && path[i] != '.' && path[i] != '[') i++
                    val key = path.substring(start, i)
                    if (key.isNotEmpty()) tokens.add(PathToken.Key(key))
                }
                '[' -> {
                    i++
                    val start = i
                    while (i < path.length && path[i] != ']') i++
                    val idx = path.substring(start, i).toIntOrNull() ?: return emptyList()
                    tokens.add(PathToken.Index(idx))
                    if (i < path.length && path[i] == ']') i++
                }
                else -> i++
            }
        }
        return tokens
    }

    companion object {
        fun from(message: String) = PathFilterBuilder(message)
    }
}
