package io.behzodhalil.togglecore.source

import io.behzodhalil.togglecore.core.FeatureFlag
import io.behzodhalil.togglecore.error.YamlParseException

/**
 * YAML parser specialized for feature flag configurations.
 *
 * - Parses only the feature flag subset of YAML
 * - Minimizes binary size (no heavy YAML library dependency)
 *
 * ### Simple boolean flags
 * ```yaml
 * features:
 *   dark_mode: true
 *   new_ui: false
 *   beta_features: on
 * ```
 *
 * ###  Complex flags with metadata
 * ```yaml
 * features:
 *   feature_name:
 *     enabled: true
 *     description: "Feature description"
 *     metadata:
 *       key1: value1
 *       key2: value2
 * ```
 *
 * ### Limitations
 *
 * - Only supports 2-space indentation (YAML standard)
 * - No support for anchors, aliases, or advanced YAML features
 * - Comments (`#`) are supported and ignored
 * - Strings with quotes are automatically unquoted
 *
 *
 * @property content YAML content to parse (must not be blank)
 *
 * @throws YamlParseException if content is invalid or malformed
 *
 * @see YamlSource
 */
internal class YamlFeatureParser(private val content: String) {

    private val lines: List<String> = content.lines()
    private var currentLineIndex: Int = 0

    /**
     * Parses YAML content into a map of feature flags.
     *
     * @return Map of feature key to FeatureFlag
     * @throws YamlParseException on parsing errors with line context
     */
    fun parse(): Map<String, FeatureFlag> {
        val features = mutableMapOf<String, FeatureFlag>()

        var inFeaturesSection = false
        var featuresIndentation = 0

        while (currentLineIndex < lines.size) {
            val line = lines[currentLineIndex]
            val trimmed = line.trim()
            val indentation = line.countLeadingSpaces()

            // Skip empty lines and comments
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                currentLineIndex++
                continue
            }

            when {
                // Found features section
                trimmed == "features:" -> {
                    inFeaturesSection = true
                    featuresIndentation = indentation
                    currentLineIndex++
                }

                // Inside features section
                inFeaturesSection -> {
                    // Exit section if dedented
                    if (indentation <= featuresIndentation && !trimmed.startsWith("features:")) {
                        inFeaturesSection = false
                        continue
                    }

                    // Parse direct children only (one level deeper = 2 spaces)
                    if (trimmed.contains(":") && indentation == featuresIndentation + 2) {
                        val parts = trimmed.split(":", limit = 2)
                        if (parts.size != 2) {
                            throwParseError("Invalid feature definition", currentLineIndex)
                        }

                        val key = parts[0].trim()
                        val valueAfterColon = parts[1].trim()

                        if (key.isBlank()) {
                            throwParseError("Feature key cannot be blank", currentLineIndex)
                        }

                        if (valueAfterColon.isEmpty()) {
                            // Multi-line complex feature
                            val featureData = parseComplexFeature(currentLineIndex + 1, indentation)
                            features[key] = featureData.toFeatureFlag(key)
                            currentLineIndex = featureData.lastLineIndex
                        } else {
                            // Single-line simple feature - strip inline comments
                            val cleanValue = stripInlineComment(valueAfterColon)
                            features[key] = FeatureFlag(
                                key = key,
                                enabled = parseBoolean(cleanValue, currentLineIndex),
                                source = SOURCE_NAME
                            )
                            currentLineIndex++
                        }
                    } else {
                        currentLineIndex++
                    }
                }

                else -> currentLineIndex++
            }
        }

        // Validate at least one feature exists
        if (features.isEmpty()) {
            throwParseError(
                "YAML must contain at least one feature in 'features:' section",
                lineNumber = null
            )
        }

        return features
    }

    /**
     * Parses a complex feature definition with enabled state, description, and metadata.
     *
     * Example input:
     * ```yaml
     *     enabled: true
     *     description: "Feature description"
     *     metadata:
     *       owner: "team-name"
     *       rollout: "50%"
     * ```
     *
     * @param startIndex Line index to start parsing from
     * @param parentIndentation Indentation of parent feature key
     * @return ComplexFeatureData containing parsed fields and final line index
     */
    private fun parseComplexFeature(startIndex: Int, parentIndentation: Int): ComplexFeatureData {
        var enabled = false
        var description: String? = null
        val metadata = mutableMapOf<String, String>()
        var lineIndex = startIndex

        while (lineIndex < lines.size) {
            val line = lines[lineIndex]
            val trimmed = line.trim()
            val indentation = line.countLeadingSpaces()

            // Exit if dedented back to parent level
            if (indentation <= parentIndentation && trimmed.isNotEmpty()) {
                break
            }

            // Skip empty lines and comments
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                lineIndex++
                continue
            }

            when {
                trimmed.startsWith("enabled:") -> {
                    val value = trimmed.substringAfter("enabled:").trim()
                    val cleanValue = stripInlineComment(value)
                    enabled = parseBoolean(cleanValue, lineIndex)
                }

                trimmed.startsWith("description:") -> {
                    val value = trimmed.substringAfter("description:").trim()
                    val cleanValue = stripInlineComment(value)
                    description = cleanValue
                        .removeSurrounding("\"")
                        .removeSurrounding("'")
                }

                trimmed.startsWith("metadata:") -> {
                    lineIndex = parseMetadataSection(lineIndex + 1, indentation, metadata)
                    continue
                }

                else -> {
                    // Unknown field - skip it (forward compatibility)
                    lineIndex++
                    continue
                }
            }

            lineIndex++
        }

        return ComplexFeatureData(
            enabled = enabled,
            description = description,
            metadata = metadata,
            lastLineIndex = lineIndex
        )
    }

    /**
     * Parses metadata section (nested key-value pairs).
     *
     * Example input:
     * ```yaml
     *       owner: "team-name"
     *       rollout: "50%"
     *       since: "2024-01-15"
     * ```
     *
     * @param startIndex Line index to start parsing from
     * @param parentIndentation Indentation of "metadata:" keyword
     * @param output Mutable map to populate with metadata entries
     * @return Line index after metadata section ends
     */
    private fun parseMetadataSection(
        startIndex: Int,
        parentIndentation: Int,
        output: MutableMap<String, String>
    ): Int {
        var lineIndex = startIndex

        while (lineIndex < lines.size) {
            val line = lines[lineIndex]
            val trimmed = line.trim()
            val indentation = line.countLeadingSpaces()

            // Exit if dedented back to parent or higher
            if (indentation <= parentIndentation && trimmed.isNotEmpty()) {
                return lineIndex - 1
            }

            // Skip empty lines and comments
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                lineIndex++
                continue
            }

            // Parse key-value pair
            if (trimmed.contains(":")) {
                val parts = trimmed.split(":", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim()
                    val value = parts[1].trim()
                        .removeSurrounding("\"")
                        .removeSurrounding("'")

                    if (key.isNotBlank()) {
                        output[key] = value
                    }
                }
            }

            lineIndex++
        }

        return lineIndex - 1
    }

    /**
     * Strips inline comments from a value string.
     *
     * Handles YAML inline comments that start with `#` outside of quoted strings.
     * This is a simplified implementation that doesn't handle quotes perfectly,
     * but works for our feature flag use case.
     *
     * **Examples**:
     * - `"true  # comment"` → `"true"`
     * - `"false"` → `"false"`
     * - `"feature # not a comment"` → `"feature"` (conservative approach)
     *
     * @param value String value that may contain inline comment
     * @return Value with inline comment removed and trimmed
     */
    private fun stripInlineComment(value: String): String {
        val commentIndex = value.indexOf('#')
        return if (commentIndex >= 0) {
            value.substring(0, commentIndex).trim()
        } else {
            value.trim()
        }
    }

    /**
     * Parses boolean value from string with flexible format support.
     *
     * **Accepted values**:
     * - `true`, `yes`, `on`, `1` → true
     * - `false`, `no`, `off`, `0` → false
     *
     * Case-insensitive matching.
     *
     * @param value String value to parse
     * @param lineNumber Line number for error reporting
     * @return Parsed boolean value
     * @throws YamlParseException if value is not a recognized boolean format
     */
    private fun parseBoolean(value: String, lineNumber: Int): Boolean {
        return when (value.lowercase().trim()) {
            "true", "yes", "on", "1" -> true
            "false", "no", "off", "0" -> false
            else -> throwParseError("Invalid boolean value: '$value'. Expected: true/false, yes/no, on/off, 1/0", lineNumber)
        }
    }

    /**
     * Throws YamlParseException with line context.
     *
     * @param message Error message
     * @param lineNumber Line number where error occurred (null for general errors)
     * @throws YamlParseException always
     */
    private fun throwParseError(message: String, lineNumber: Int?): Nothing {
        throw YamlParseException(
            message = message,
            line = lineNumber?.let { it + 1 } // Convert 0-based to 1-based for user display
        )
    }

    /**
     * Internal data class for complex feature parsing results.
     *
     * @property enabled Whether the feature is enabled
     * @property description Optional feature description
     * @property metadata Map of custom metadata key-value pairs
     * @property lastLineIndex Last line index processed (used to resume parsing)
     */
    private data class ComplexFeatureData(
        val enabled: Boolean,
        val description: String?,
        val metadata: Map<String, String>,
        val lastLineIndex: Int
    ) {
        /**
         * Converts parsed data into FeatureFlag instance.
         *
         * Merges description into metadata map if present.
         */
        fun toFeatureFlag(key: String): FeatureFlag {
            val allMetadata = metadata.toMutableMap()
            description?.let { allMetadata["description"] = it }

            return FeatureFlag(
                key = key,
                enabled = enabled,
                source = SOURCE_NAME,
                metadata = allMetadata
            )
        }
    }

    private companion object {
        private const val SOURCE_NAME = "yaml"
    }
}

/**
 * Counts leading space characters in a string.
 *
 * Used for YAML indentation parsing.
 *
 * @return Number of leading spaces
 */
internal fun String.countLeadingSpaces(): Int = takeWhile { it == ' ' }.length

/**
 * Attempts to extract line number from exception message.
 *
 * Parses messages like "... at line 42" or "... line 42:".
 *
 * @param exception Exception to extract from
 * @return Line number if found, null otherwise
 */
internal fun extractLineNumberFromException(exception: Exception): Int? {
    return exception.message
        ?.substringAfter("line ", "")
        ?.takeWhile { it.isDigit() }
        ?.toIntOrNull()
}