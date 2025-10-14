package io.behzodhalil.togglecore.observer

/**
 * Observer for feature flag changes.
 *
 * Implement [FeatureObserver] to react to feature flag changes in real-time.
 *
 * ### Usage Example
 *
 * ```kotlin
 * val observer = object : FeatureObserver {
 *     override fun onChanged(key: String, oldValue: Boolean, newValue: Boolean) {
 *         println("Feature $key changed: $oldValue → $newValue")
 *
 *         when (key) {
 *             "dark_mode" -> if (newValue) applyDarkTheme() else applyLightTheme()
 *             "premium" -> if (newValue) showPremiumFeatures()
 *         }
 *     }
 * }
 *
 * val unsubscribe = manager.addObserver("dark_mode", observer)
 *
 * // Later, when done observing
 * unsubscribe()
 * ```
 *
 * ### Thread Safety
 *
 * Observer methods may be called from any thread. Implementations should
 * handle threading appropriately (e.g., dispatch to main thread for UI updates).
 *
 */
public fun interface FeatureObserver {
    /**
     * Called when a feature flag value changes.
     *
     * @param key Feature key that changed
     * @param oldValue Previous value
     * @param newValue New value
     */
    public fun onChanged(
        key: String,
        oldValue: Boolean,
        newValue: Boolean,
    )
}

/**
 * Event representing a feature flag change.
 *
 * @property key Feature key that changed
 * @property oldValue Previous value
 * @property newValue New value
 * @property timestamp When the change occurred (millis)
 */
public data class FeatureChangeEvent(
    val key: String,
    val oldValue: Boolean,
    val newValue: Boolean,
    val timestamp: Long,
)
