package io.behzodhalil.togglecore.source

import io.behzodhalil.togglecore.core.ToggleInternal

@ToggleInternal
class SourcesScope internal constructor() {
    private val sources = mutableListOf<FeatureSource>()

    /**
     * Add in-memory source with DSL configuration
     */
    fun memory(configure: MemorySourceScope.() -> Unit = {}) {
        val builder = MemorySourceScope()
        builder.configure()
        sources.add(builder.build())
    }

    /**
     * Add YAML source with DSL configuration
     */
    fun yaml(configure: YamlSourceScope.() -> Unit) {
        val builder = YamlSourceScope()
        builder.configure()
        sources.add(builder.build())
    }

    /**
     * Add custom source
     */
    fun new(source: FeatureSource) {
        sources.add(source)
    }

    /**
     * Add custom source with priority
     */
    fun new(priority: Int, sourceFactory: () -> FeatureSource) {
        val source = sourceFactory()
        sources.add(PriorityFeatureSource(source, priority))
    }

    internal fun build(): List<FeatureSource> = sources.toList()
}