package io.behzodhalil.togglecore.source.yaml

import io.behzodhalil.togglecore.core.ToggleInternal
import io.behzodhalil.togglecore.source.FeatureSource

@ToggleInternal
public class YamlSourceScope {
    private var resourcePath: String? = null
    private var content: String? = null

    /**
     * Load from resource path
     */
    public fun fromResource(path: String) {
        this.resourcePath = path
    }

    /**
     * Load from string content
     */
    public fun fromString(content: String) {
        this.content = content
    }

    internal fun build(): FeatureSource {
        return when {
            resourcePath != null -> YamlSource.fromResource(resourcePath!!)
            content != null -> YamlSource.fromString(content!!)
            else -> throw IllegalArgumentException("Either resourcePath or content must be specified for YAML source")
        }
    }
}
