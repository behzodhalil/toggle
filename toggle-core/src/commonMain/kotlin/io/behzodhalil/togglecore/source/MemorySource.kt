package io.behzodhalil.togglecore.source

import io.behzodhalil.togglecore.core.FeatureFlag
import io.behzodhalil.togglecore.core.FeatureKey

public class MemorySource(
    features: Map<String, Boolean> = emptyMap(),
) : FeatureSource {
    override val sourceName: String = DEFAULT_SOURCE_NAME
    override val priority: Int = DEFAULT_MEMORY_PRIORITY

    private val features = features.toMutableMap()

    override fun get(key: String): FeatureFlag? {
        return features[key]?.let { enabled ->
            FeatureFlag(key, enabled, "memory")
        }
    }

    public fun setFeature(
        key: String,
        enabled: Boolean,
    ) {
        features[key] = enabled
    }

    public fun setFeature(
        feature: FeatureKey,
        enabled: Boolean,
    ) {
        setFeature(feature.value, enabled)
    }

    public fun removeFeature(key: String) {
        features.remove(key)
    }

    public fun clear() {
        features.clear()
    }

    override fun getAll(): List<FeatureFlag> {
        return features.map { (key, enabled) ->
            FeatureFlag(key, enabled, "memory")
        }
    }

    public companion object {
        private const val DEFAULT_MEMORY_PRIORITY = 200
        private const val DEFAULT_SOURCE_NAME = "memory_source"
    }
}
