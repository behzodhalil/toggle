package io.behzodhalil.togglecore.error

/**
 * Exception thrown when YAML parsing fails.
 *
 * @property line Line number where error occurred (1-based, null for structural errors)
 * @property message Human-readable error description with line context
 * @property cause Underlying exception that triggered the error (if any)
 *
 * @see ParseException
 */
public class YamlParseException(
    message: String,
    cause: Throwable? = null,
    public val line: Int? = null,
) : ParseException(
        message =
            buildString {
                append(message)
                line?.let { append(" at line $it") }
            },
        cause = cause,
    )
