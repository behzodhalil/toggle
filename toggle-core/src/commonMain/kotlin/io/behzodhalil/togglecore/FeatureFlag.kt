package io.behzodhalil.togglecore

import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Immutable feature flag state.
 *
 * ```kotlin
 * val flag = FeatureFlag.enabled("dark_mode", "remote")
 * if (flag.enabled) { /* ... */ }
 * ```
 */
@OptIn(ExperimentalTime::class)
@Serializable
data class FeatureFlag(
    val key: String,
    val enabled: Boolean,
    val source: String,
    val metadata: Map<String, String> = emptyMap(),
    val timestamp: Long = Clock.System.now().toEpochMilliseconds()
) {
    companion object {
        /** Create enabled flag. */
        fun enabled(key: String, source: String = "default") = FeatureFlag(key, true, source)

        /** Create disabled flag. */
        fun disabled(key: String, source: String = "default") = FeatureFlag(key, false, source)
    }
}
