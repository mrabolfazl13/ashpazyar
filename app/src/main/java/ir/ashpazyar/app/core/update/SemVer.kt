package ir.ashpazyar.app.core.update

/**
 * Strict Semantic Versioning 2.0.0 parser and comparator.
 *
 * Build metadata is ignored for precedence. Pre-release versions sort before
 * the corresponding release, and numeric identifiers sort before text ones.
 */
data class SemVer private constructor(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val preRelease: List<String>,
    val buildMetadata: List<String>,
    val source: String
) : Comparable<SemVer> {

    override fun compareTo(other: SemVer): Int {
        major.compareTo(other.major).takeIf { it != 0 }?.let { return it }
        minor.compareTo(other.minor).takeIf { it != 0 }?.let { return it }
        patch.compareTo(other.patch).takeIf { it != 0 }?.let { return it }
        if (preRelease.isEmpty() && other.preRelease.isEmpty()) return 0
        if (preRelease.isEmpty()) return 1
        if (other.preRelease.isEmpty()) return -1
        val size = minOf(preRelease.size, other.preRelease.size)
        for (index in 0 until size) {
            val left = preRelease[index]
            val right = other.preRelease[index]
            val leftNumeric = left.all(Char::isDigit)
            val rightNumeric = right.all(Char::isDigit)
            val result = when {
                leftNumeric && rightNumeric -> compareNumericIdentifiers(left, right)
                leftNumeric -> -1
                rightNumeric -> 1
                else -> left.compareTo(right)
            }
            if (result != 0) return result
        }
        return preRelease.size.compareTo(other.preRelease.size)
    }

    override fun toString(): String = source

    private fun compareNumericIdentifiers(left: String, right: String): Int {
        left.length.compareTo(right.length).takeIf { it != 0 }?.let { return it }
        return left.compareTo(right)
    }

    companion object {
        private val NUMBER = Regex("0|[1-9][0-9]*")
        private val IDENTIFIER = Regex("[0-9A-Za-z-]+")

        fun parse(raw: String): SemVer? {
            val value = raw.trim()
            if (value.length !in 1..128) return null
            val plus = value.indexOf('+')
            val coreAndPre = if (plus >= 0) value.substring(0, plus) else value
            val build = if (plus >= 0) value.substring(plus + 1) else ""
            if (plus >= 0 && build.isBlank()) return null
            val buildParts = if (build.isBlank()) emptyList() else build.split('.')
            if (buildParts.any { it.isBlank() || !IDENTIFIER.matches(it) }) return null

            val dash = coreAndPre.indexOf('-')
            val core = if (dash >= 0) coreAndPre.substring(0, dash) else coreAndPre
            val pre = if (dash >= 0) coreAndPre.substring(dash + 1) else ""
            if (dash >= 0 && pre.isBlank()) return null
            val parts = core.split('.')
            if (parts.size != 3 || parts.any { !NUMBER.matches(it) }) return null
            val numbers = parts.map { it.toIntOrNull() ?: return null }
            val preParts = if (pre.isBlank()) emptyList() else pre.split('.')
            if (preParts.any { it.isBlank() || !IDENTIFIER.matches(it) }) return null
            if (preParts.any { it.all(Char::isDigit) && it.length > 1 && it.startsWith('0') }) return null
            return SemVer(numbers[0], numbers[1], numbers[2], preParts, buildParts, value)
        }
    }
}
