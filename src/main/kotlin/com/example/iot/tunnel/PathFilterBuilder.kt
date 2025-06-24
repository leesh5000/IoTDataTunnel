package com.example.iot.tunnel

import com.jayway.jsonpath.JsonPath
import kotlin.jvm.javaPrimitiveType

/**
 * Filters JSON messages using JSONPath expressions and extracts values when all
 * path filters match.
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
        val ctx = try {
            JsonPath.parse(message)
        } catch (_: Exception) {
            return null
        }
        for ((path, expected) in pathFilters) {
            val actual = try {
                ctx.read<Any>(path, Any::class.java)
            } catch (_: Exception) {
                return null
            }
            if (actual is Number && expected is Number) {
                if (actual.toDouble() != expected.toDouble()) return null
            } else if (actual != expected) {
                return null
            }
        }
        for (path in valueFilters) {
            val raw = try {
                ctx.read<Any>(path)
            } catch (_: Exception) {
                null
            }
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
                }
            }
        }
        return null
    }

    companion object {
        fun from(message: String) = PathFilterBuilder(message)
    }
}
