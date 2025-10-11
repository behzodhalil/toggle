package io.behzodhalil.togglecore.source

import io.behzodhalil.togglecore.core.FeatureFlag
import io.behzodhalil.togglecore.core.FeatureKey

class MemorySource(
    features: Map<String, Boolean> = emptyMap(),
) : FeatureSource {

    override val sourceName: String = DEFAULT_SOURCE_NAME
    override val priority: Int = DEFAULT_MEMORY_PRIORITY

    private val _features = features.toMutableMap()

    override fun get(key: String): FeatureFlag? {
        return _features[key]?.let { enabled ->
            FeatureFlag(key, enabled, "memory")
        }
    }

    fun setFeature(key: String, enabled: Boolean) {
        _features[key] = enabled
    }

    fun setFeature(feature: FeatureKey, enabled: Boolean) {
        setFeature(feature.value, enabled)
    }

    fun removeFeature(key: String) {
        _features.remove(key)
    }

    fun clear() {
        _features.clear()
    }

    override fun getAll(): List<FeatureFlag> {
        return _features.map { (key, enabled) ->
            FeatureFlag(key, enabled, "memory")
        }
    }

    companion object {
        private const val DEFAULT_MEMORY_PRIORITY = 200
        private const val DEFAULT_SOURCE_NAME = "memory_source"
    }
}