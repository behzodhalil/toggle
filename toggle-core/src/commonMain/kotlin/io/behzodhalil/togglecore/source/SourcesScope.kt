package io.behzodhalil.togglecore.source

import io.behzodhalil.togglecore.core.ToggleInternal

@ToggleInternal
public class SourcesScope internal constructor() {
    private val sources = mutableListOf<FeatureSource>()

    /**
     * Add in-memory source with DSL configuration
     */
    public fun memory(configure: MemorySourceScope.() -> Unit = {}) {
        val builder = MemorySourceScope()
        builder.configure()
        sources.add(builder.build())
    }

    /**
     * Add YAML source with DSL configuration
     */
    public fun yaml(configure: YamlSourceScope.() -> Unit) {
        val builder = YamlSourceScope()
        builder.configure()
        sources.add(builder.build())
    }

    /**
     * Add custom source
     */
    public fun new(source: FeatureSource) {
        sources.add(source)
    }

    /**
     * Add custom source with priority
     */
    public fun new(priority: Int, sourceFactory: () -> FeatureSource) {
        val source = sourceFactory()
        sources.add(PriorityFeatureSource(source, priority))
    }

    internal fun build(): List<FeatureSource> = sources.toList()
}