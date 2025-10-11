package io.behzodhalil.togglecore.core

import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Immutable value object representing a feature flag's state at a point in time.
 *
 * Each flag captures:
 * - **key**: Feature identifier (e.g., "dark_mode")
 * - **enabled**: Boolean state (true/false)
 * - **source**: Where the flag came from (e.g., "remote", "yaml", "memory")
 * - **metadata**: Additional contextual information
 * - **timestamp**: When the flag was created/evaluated
 *
 * ### Example
 * ```kotlin
 * val flag = FeatureFlag.enabled("dark_mode", "remote")
 * if (flag.enabled) { /* ... */ }
 * ```
 *
 * @property key Feature identifier (must be non-blank)
 * @property enabled Whether the feature is enabled
 * @property source Source that provided this flag (e.g., "remote", "yaml", "memory")
 * @property metadata Immutable map of additional contextual information
 * @property timestamp Unix timestamp (milliseconds) when flag was created/evaluated
 *
 * @see FeatureKey
 */
@OptIn(ExperimentalTime::class)
@Serializable
data class FeatureFlag(
    val key: String,
    val enabled: Boolean,
    val source: String,
    val metadata: Map<String, String> = emptyMap(),
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
) {

    init {
        require(key.isNotBlank()) {
            "Feature flag key cannot be blank"
        }
        require(source.isNotBlank()) {
            "Feature flag source cannot be blank"
        }
    }

    /**
     * Creates with a new enabled state.
     *
     * ## Example
     * ```kotlin
     * val disabled = flag.withEnabled(false)
     * val enabled = flag.withEnabled(true)
     * ```
     *
     * @param enabled New enabled state
     * @return New FeatureFlag instance with updated state
     */
    fun withEnabled(enabled: Boolean): FeatureFlag {
        return copy(enabled = enabled, timestamp = Clock.System.now().toEpochMilliseconds())
    }

    /**
     * Creates with a new source.
     *
     * Useful when a flag is overridden by a different source.
     *
     * @param source New source name
     * @return New FeatureFlag instance with updated source
     */
    fun withSource(source: String): FeatureFlag {
        require(source.isNotBlank()) { "Source cannot be blank" }
        return copy(source = source, timestamp = Clock.System.now().toEpochMilliseconds())
    }


    /**
     * Creates with an additional metadata (bulk operation).
     *
     * @param metadata Map of metadata to add/update
     * @return New FeatureFlag with merged metadata
     */
    fun withMetadata(metadata: Map<String, String>): FeatureFlag {
        if (metadata.isEmpty()) return this
        return copy(
            metadata = this.metadata + metadata,
            timestamp = Clock.System.now().toEpochMilliseconds()
        )
    }

    /**
     * Pretty-printed string representation.
     *
     * Format: `FeatureFlag(key=X, enabled=Y, source=Z, metadata={...})`
     *
     * @return Human-readable representation
     */
    override fun toString(): String = buildString {
        append("FeatureFlag(")
        append("key=$key, ")
        append("enabled=$enabled, ")
        append("source=$source")
        if (metadata.isNotEmpty()) {
            append(", metadata={")
            append(metadata.entries.joinToString(", ") { "${it.key}=${it.value}" })
            append("}")
        }
        append(")")
    }

    companion object {
        fun enabled(key: String, source: String = "default") = FeatureFlag(key, true, source)
        fun disabled(key: String, source: String = "default") = FeatureFlag(key, false, source)
    }
}
