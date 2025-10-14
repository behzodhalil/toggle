package io.behzodhalil.togglecore.error

/**
 * Base exception for parsing errors in feature sources.
 *
 * Thrown when feature flag data cannot be parsed from its source format (YAML, JSON, etc.).
 *
 * ### Example
 *
 * Extend this class for source-specific parsing exceptions:
 * ```kotlin
 * class YamlParseException(
 *     message: String,
 *     cause: Throwable? = null,
 *     val line: Int? = null
 * ) : ParseException(message, cause)
 * ```
 *
 * @param message Error description
 * @param cause Underlying exception that triggered the error (if any)
 *
 * @see YamlParseException
 */
public open class ParseException(message: String, cause: Throwable? = null) : Exception(message, cause)
