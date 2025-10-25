package io.behzodhalil.togglecore.source

import io.behzodhalil.togglecore.core.FeatureFlag
import io.behzodhalil.togglecore.core.FeatureKey
import kotlinx.atomicfu.atomic
import kotlinx.collections.immutable.toPersistentMap

public class MemorySource(
    features: Map<String, Boolean> = emptyMap(),
) : FeatureSource {
    override val sourceName: String = DEFAULT_SOURCE_NAME
    override val priority: Int = DEFAULT_MEMORY_PRIORITY

    private val features = atomic( features.toPersistentMap())

    override fun get(key: String): FeatureFlag? {
        return features.value[key]?.let { enabled ->
            FeatureFlag(key, enabled, "memory")
        }
    }

    public fun setFeature(
        key: String,
        enabled: Boolean,
    ) {
        features.value.put(key, enabled)
    }

    public fun setFeature(
        feature: FeatureKey,
        enabled: Boolean,
    ) {
        setFeature(feature.value, enabled)
    }

    public fun removeFeature(key: String) {
        features.value.remove(key)
    }

    public fun clear() {
        features.value.clear()
    }

    override fun getAll(): List<FeatureFlag> {
        return features.value.map { (key, enabled) ->
            FeatureFlag(key, enabled, "memory")
        }
    }

    public companion object {
        private const val DEFAULT_MEMORY_PRIORITY = 200
        private const val DEFAULT_SOURCE_NAME = "memory_source"
    }
}
