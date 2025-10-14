package io.behzodhalil.togglecore

import io.behzodhalil.togglecore.core.FeatureFlag
import io.behzodhalil.togglecore.source.FeatureSource
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FeatureSourceTest {
    @Test
    fun `feature source interface default implementations`() {
        val basicSource =
            object : FeatureSource {
                override val sourceName: String = "custom"

                override fun get(key: String): FeatureFlag? = null
            }

        // Test default implementations
        assertEquals(0, basicSource.priority)
        assertTrue(basicSource.getAll().isEmpty())

        // Test refresh doesn't throw
        runBlocking { basicSource.refresh() }
    }

    @Test
    fun `custom feature source implementation`() {
        val customSource =
            object : FeatureSource {
                override val priority = 150
                override val sourceName: String = "custom"

                override fun get(key: String): FeatureFlag? {
                    return if (key == "custom_feature") {
                        FeatureFlag(key, true, "custom")
                    } else {
                        null
                    }
                }

                override fun getAll(): List<FeatureFlag> {
                    return listOf(FeatureFlag("custom_feature", true, "custom"))
                }
            }

        assertEquals(150, customSource.priority)

        val feature = customSource.get("custom_feature")
        assertEquals("custom_feature", feature?.key)
        assertEquals(true, feature?.enabled)
        assertEquals("custom", feature?.source)

        assertNull(customSource.get("nonexistent"))

        val allFeatures = customSource.getAll()
        assertEquals(1, allFeatures.size)
        assertEquals("custom_feature", allFeatures[0].key)
    }

    @Test
    fun `feature source with async refresh`() {
        var refreshCalled = false
        val asyncSource =
            object : FeatureSource {
                override val sourceName: String = "custom"

                override fun get(key: String): FeatureFlag? = null

                override suspend fun refresh() {
                    refreshCalled = true
                }
            }

        runBlocking { asyncSource.refresh() }
        assertTrue(refreshCalled)
    }

    @Test
    fun `feature source priority comparison`() {
        val highPrioritySource =
            object : FeatureSource {
                override val priority = 300
                override val sourceName: String = "custom"

                override fun get(key: String): FeatureFlag? = null
            }

        val lowPrioritySource =
            object : FeatureSource {
                override val priority = 100
                override val sourceName: String = "custom"

                override fun get(key: String): FeatureFlag? = null
            }

        val defaultPrioritySource =
            object : FeatureSource {
                override val sourceName: String = "custom"

                override fun get(key: String): FeatureFlag? = null
            }

        assertTrue(highPrioritySource.priority > lowPrioritySource.priority)
        assertTrue(lowPrioritySource.priority > defaultPrioritySource.priority)
    }

    @Test
    fun `feature source with metadata`() {
        val metadataSource =
            object : FeatureSource {
                override val sourceName: String = "custom"

                override fun get(key: String): FeatureFlag? {
                    return if (key == "metadata_feature") {
                        FeatureFlag(
                            key = key,
                            enabled = true,
                            source = "metadata_source",
                            metadata =
                                mapOf(
                                    "version" to "2.0",
                                    "experiment" to "test_exp",
                                    "rollout" to "30%",
                                ),
                        )
                    } else {
                        null
                    }
                }
            }

        val feature = metadataSource.get("metadata_feature")
        assertEquals("2.0", feature?.metadata?.get("version"))
        assertEquals("test_exp", feature?.metadata?.get("experiment"))
        assertEquals("30%", feature?.metadata?.get("rollout"))
    }

    @Test
    fun `feature source returning null for unknown keys`() {
        val nullSource =
            object : FeatureSource {
                override val sourceName: String = "custom"

                override fun get(key: String): FeatureFlag? = null
            }

        assertNull(nullSource.get("any_key"))
        assertNull(nullSource.get(""))
        assertNull(nullSource.get("unknown"))
    }

    @Test
    fun `feature source with conditional logic`() {
        val conditionalSource =
            object : FeatureSource {
                override val sourceName: String = "custom"
                private val enabledFeatures = setOf("enabled_1", "enabled_2", "enabled_3")

                override fun get(key: String): FeatureFlag? {
                    return if (key in enabledFeatures) {
                        FeatureFlag(key, true, "conditional")
                    } else if (key.startsWith("disabled_")) {
                        FeatureFlag(key, false, "conditional")
                    } else {
                        null
                    }
                }

                override fun getAll(): List<FeatureFlag> {
                    return enabledFeatures.map { FeatureFlag(it, true, "conditional") } +
                        listOf(FeatureFlag("disabled_example", false, "conditional"))
                }
            }

        // Test enabled features
        assertTrue(conditionalSource.get("enabled_1")?.enabled ?: false)
        assertTrue(conditionalSource.get("enabled_2")?.enabled ?: false)

        // Test disabled features
        assertFalse(conditionalSource.get("disabled_test")?.enabled ?: true)

        // Test unknown features
        assertNull(conditionalSource.get("unknown_feature"))

        // Test get all features
        val allFeatures = conditionalSource.getAll()
        assertTrue(allFeatures.size >= 4)
    }
}
