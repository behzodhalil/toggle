package io.behzodhalil.togglecore.core

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlin.collections.plus
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmStatic

/**
 * Type-safe, immutable identifier for a feature flag.
 *
 * Each instance is automatically registered for debugging. Keys must be non-blank.
 *
 * ```kotlin
 * val darkMode = FeatureKey.DARK_MODE
 * val betaUi = FeatureKey.BETA_UI
 * ```
 */

@JvmInline
public value class FeatureKey private constructor(public val value: String) :
    Comparable<FeatureKey> {
    init {
        require(value.isNotBlank()) {
            "Feature key cannot be blank. Use meaningful identifiers like 'dark_mode' or 'premium_feature'."
        }
    }

    /**
     * Compares keys lexicographically by their string value.
     *
     * Useful for sorted displays and consistent ordering.
     */
    override fun compareTo(other: FeatureKey): Int {
        return value.compareTo(other.value)
    }

    public companion object Registry {
        private val _registry = atomic(emptySet<FeatureKey>())
        public val registry: Set<FeatureKey>
            get() = _registry.value


        @JvmStatic
        public fun of(value: String): FeatureKey {
            val key = FeatureKey(value)
            register(key)
            return key
        }

        /**
         * Removes a key from the registry.
         * Call this when you no longer need a dynamic feature key.
         */
        @JvmStatic
        public fun unregister(key: FeatureKey) {
            _registry.update { it - key }
        }

        /**
         * Clears all keys from the registry.
         * Useful for testing or application shutdown.
         */
        @JvmStatic
        public fun clear() {
            _registry.update { emptySet() }
        }


        @JvmStatic
        public fun find(value: String): FeatureKey? {
            return _registry.value.find { it.value == value }
        }

        private fun register(key: FeatureKey) {
            _registry.update { current ->
                current + key
            }
        }

        public val EXPERIMENTAL_API: FeatureKey = of("experimental_api")
        public val BETA_UI: FeatureKey = of("beta_ui")
    }

    override fun toString(): String = "FeatureKey($value)"
}