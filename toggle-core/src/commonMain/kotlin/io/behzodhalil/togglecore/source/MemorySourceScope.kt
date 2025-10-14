package io.behzodhalil.togglecore.source

import io.behzodhalil.togglecore.core.FeatureKey
import io.behzodhalil.togglecore.core.ToggleInternal

@ToggleInternal
public class MemorySourceScope {
    private val features = mutableMapOf<String, Boolean>()

    /**
     * Add feature by string key
     */
    public fun feature(
        key: String,
        enabled: Boolean,
    ) {
        features[key] = enabled
    }

    /**
     * Add feature by FeatureKey
     */
    public fun feature(
        key: FeatureKey,
        enabled: Boolean,
    ) {
        features[key.value] = enabled
    }

    /**
     * Add multiple features
     */
    public fun features(vararg pairs: Pair<String, Boolean>) {
        features.putAll(pairs)
    }

    /**
     * Add features from map
     */
    public fun features(map: Map<String, Boolean>) {
        features.putAll(map)
    }

    internal fun build(): FeatureSource = MemorySource(features)
}
