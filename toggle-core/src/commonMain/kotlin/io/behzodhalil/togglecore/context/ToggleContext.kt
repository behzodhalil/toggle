package io.behzodhalil.togglecore.context

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
public data class ToggleContext(
    val userId: String? = null,
    val country: String? = null,
    val language: String? = null,
    val appVersion: String? = null,
    val deviceId: String? = null,
    val attributes: Map<String, AttributeValue> = emptyMap(),
)

public fun ToggleContext.getStringAttribute(
    key: String,
    default: String? = null,
): String? {
    return (attributes[key] as? AttributeValue.StringValue)?.value ?: default
}

public fun ToggleContext.getBooleanAttribute(
    key: String,
    default: Boolean = false,
): Boolean {
    return (attributes[key] as? AttributeValue.BooleanValue)?.value ?: default
}

public fun ToggleContext.getIntAttribute(
    key: String,
    default: Int? = null,
): Int? {
    return (attributes[key] as? AttributeValue.IntValue)?.value ?: default
}

public fun ToggleContext.getDoubleAttribute(
    key: String,
    default: Double? = null,
): Double? {
    return (attributes[key] as? AttributeValue.DoubleValue)?.value ?: default
}

public fun ToggleContext.getLongAttribute(
    key: String,
    default: Long? = null,
): Long? {
    return (attributes[key] as? AttributeValue.LongValue)?.value ?: default
}

public fun ToggleContext.getShortAttribute(
    key: String,
    default: Short? = null,
): Short? {
    return (attributes[key] as? AttributeValue.ShortValue)?.value ?: default
}
