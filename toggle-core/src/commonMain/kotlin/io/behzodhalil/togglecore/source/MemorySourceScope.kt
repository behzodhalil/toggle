package io.behzodhalil.togglecore.source

import io.behzodhalil.togglecore.core.FeatureKey
import io.behzodhalil.togglecore.core.ToggleInternal

@ToggleInternal
class MemorySourceScope {
    private val features = mutableMapOf<String, Boolean>()

    /**
     * Add feature by string key
     */
    fun feature(key: String, enabled: Boolean) {
        features[key] = enabled
    }

    /**
     * Add feature by FeatureKey
     */
    fun feature(key: FeatureKey, enabled: Boolean) {
        features[key.value] = enabled
    }

    /**
     * Add multiple features
     */
    fun features(vararg pairs: Pair<String, Boolean>) {
        features.putAll(pairs)
    }

    /**
     * Add features from map
     */
    fun features(map: Map<String, Boolean>) {
        features.putAll(map)
    }

    internal fun build(): FeatureSource = MemorySource(features)
}