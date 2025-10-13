package io.behzodhalil.togglecore.error

/**
 * Exception thrown when all sources fail during refresh.
 *
 * Contains the list of individual failures for diagnostics.
 *
 * @property failures List of exceptions from each failed source
 */
public class RefreshException(
    message: String,
    public val failures: List<Exception>,
) : RuntimeException(
    message,
    failures.firstOrNull()
) {
    override fun toString(): String {
        return buildString {
            append("RefreshException: $message")
            if (failures.isNotEmpty()) {
                append("\nFailures:")
                failures.forEachIndexed { index, error ->
                    append("\n  ${index + 1}. ${error::class.simpleName}: ${error.message}")
                }
            }
        }
    }
}