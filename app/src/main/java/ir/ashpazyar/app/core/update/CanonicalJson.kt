package ir.ashpazyar.app.core.update

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Deterministic compact JSON used as the signed byte sequence.
 *
 * Keys are sorted recursively, separators contain no whitespace, and Unicode
 * is emitted as UTF-8. The Python publisher uses the same canonical form.
 */
object CanonicalJson {

    fun encode(element: JsonElement): String = buildString { appendElement(element) }

    private fun StringBuilder.appendElement(element: JsonElement) {
        when (element) {
            is JsonObject -> {
                append('{')
                element.entries.sortedBy { it.key }.forEachIndexed { index, entry ->
                    if (index > 0) append(',')
                    append(JsonPrimitive(entry.key).toString())
                    append(':')
                    appendElement(entry.value)
                }
                append('}')
            }
            is JsonArray -> {
                append('[')
                element.forEachIndexed { index, value ->
                    if (index > 0) append(',')
                    appendElement(value)
                }
                append(']')
            }
            JsonNull -> append("null")
            is JsonPrimitive -> append(element.toString())
        }
    }
}
