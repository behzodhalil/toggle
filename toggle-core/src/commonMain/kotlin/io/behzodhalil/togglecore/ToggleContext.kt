package io.behzodhalil.togglecore

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * Context for feature evaluation.
 *
 * ```kotlin
 * val context = ToggleContext(
 *     userId = "user123",
 *     country = "US",
 *     appVersion = "1.2.0"
 * )
 * ```
 */
@Serializable
data class ToggleContext(
    val userId: String? = null,
    val country: String? = null,
    val language: String? = null,
    val appVersion: String? = null,
    val deviceId: String? = null,
    val attributes: Map<String,  @Contextual Any> = emptyMap()
)