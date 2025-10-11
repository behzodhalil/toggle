package io.behzodhalil.togglecore.source

import io.behzodhalil.togglecore.core.FeatureFlag

internal class PriorityFeatureSource(
    private val delegate: FeatureSource,
    overridePriority: Int,
) : FeatureSource {

    override val sourceName: String = DEFAULT_SOURCE_NAME
    override val priority: Int = overridePriority

    override fun get(key: String): FeatureFlag? = delegate.get(key)

    override suspend fun refresh() = delegate.refresh()

    override fun getAll(): List<FeatureFlag> = delegate.getAll()

    companion object {
        private const val DEFAULT_SOURCE_NAME = "priority_source_name"
    }
}