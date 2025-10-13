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
value class FeatureKey private constructor(val value: String): Comparable<FeatureKey> {
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

    companion object Registry {
        private val _registry = atomic(emptySet<FeatureKey>())
        val registry: Set<FeatureKey>
            get() = _registry.value


        @JvmStatic
        fun of(value: String): FeatureKey {
            val key = FeatureKey(value)
            register(key)
            return key
        }

        @JvmStatic
        fun find(value: String): FeatureKey? {
            return _registry.value.find { it.value == value }
        }

        private fun register(key: FeatureKey) {
            _registry.update { current ->
                current + key
            }
        }

        val EXPERIMENTAL_API = of("experimental_api")
        val BETA_UI = of("beta_ui")
    }

    override fun toString(): String = "FeatureKey($value)"
}