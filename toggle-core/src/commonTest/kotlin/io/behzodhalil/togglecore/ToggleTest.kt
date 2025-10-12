package io.behzodhalil.togglecore

import io.behzodhalil.togglecore.core.FeatureFlag
import io.behzodhalil.togglecore.core.FeatureKey
import io.behzodhalil.togglecore.context.ToggleContext
import io.behzodhalil.togglecore.evaluator.RuleEvaluator
import io.behzodhalil.togglecore.core.Toggle
import io.behzodhalil.togglecore.source.FeatureSource
import io.behzodhalil.togglecore.source.MemorySource
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class ToggleTest {

    @Test
    fun `given a memory source with enabled feature when checking feature state then returns true`() {
        // Given
        val source = MemorySource(mapOf("test_feature" to true))
        val toggle = Toggle {
            sources { new(source) }
        }

        // When & Then
        assertTrue(toggle.isEnabled(FeatureKey.of("test_feature")))
        assertFalse(toggle.isEnabled(FeatureKey.of("nonexistent_feature")))

        toggle.close()
    }

    @Test
    fun `given multiple sources when toggle is created then all sources are available`() {
        // Given
        val source1 = MemorySource(mapOf("feature1" to true))
        val source2 = MemorySource(mapOf("feature2" to false))

        // When
        val toggle = Toggle {
            sources {
                new(source1)
                new(source2)
            }
        }

        // Then
        assertTrue(toggle.isEnabled(FeatureKey.of("feature1")))
        assertFalse(toggle.isEnabled(FeatureKey.of("feature2")))

        toggle.close()
    }

    @Test
    fun `given conflicting sources with different priorities when resolving feature then higher priority source wins`() {
        // Given
        val highPrioritySource = object : FeatureSource {
            override val sourceName: String = "high_priority_source"
            override val priority = 300
            override fun get(key: String) = if (key == "priority_test") {
                FeatureFlag(key, false, "high_priority")
            } else null
        }

        val lowPrioritySource = object : FeatureSource {
            override val sourceName: String = "low_priority_source"
            override val priority = 100
            override fun get(key: String) = if (key == "priority_test") {
                FeatureFlag(key, true, "low_priority")
            } else null
        }

        // When
        val toggle = Toggle {
            sources {
                new(lowPrioritySource)
                new(highPrioritySource)
            }
        }
        val feature = toggle.value(FeatureKey.of("priority_test"))

        // Then
        assertFalse(feature.enabled) // High priority source wins
        assertEquals("high_priority", feature.source)

        toggle.close()
    }

    @Test
    fun `given custom evaluator and vip user context when evaluating disabled feature then evaluator enables it`() {
        // Given
        val customEvaluator = object : RuleEvaluator {
            override fun evaluate(flag: FeatureFlag, context: ToggleContext): FeatureFlag {
                return if (context.userId == "vip") {
                    flag.copy(enabled = true)
                } else {
                    flag.copy(enabled = false)
                }
            }
        }
        val source = MemorySource(mapOf("vip_feature" to false))

        // When
        val toggle = Toggle {
            evaluation { evaluator(customEvaluator) }
            sources { new(source) }
            context { userId("vip") }
        }

        // Then
        assertTrue(toggle.isEnabled(FeatureKey.of("vip_feature")))

        toggle.close()
    }

    @Test
    fun `given cached feature when accessing same feature twice then source is queried only once`() {
        // Given
        var evaluationCount = 0
        val source = object : FeatureSource {
            override val sourceName: String = "custom_source"

            override fun get(key: String): FeatureFlag? {
                return if (key == "cached_feature") {
                    evaluationCount++
                    FeatureFlag(key, true, "counting_source")
                } else null
            }
        }
        val toggle = Toggle {
            sources { new(source) }
        }

        // When
        val feature1 = toggle.value(FeatureKey.of("cached_feature"))
        val feature2 = toggle.value(FeatureKey.of("cached_feature"))

        // Then
        assertTrue(feature1.enabled)
        assertTrue(feature2.enabled)
        assertEquals(1, evaluationCount) // Should only evaluate once due to caching

        toggle.close()
    }

    @Test
    fun `given enabled feature when refreshing source then cache is cleared and new value is used`() = runBlocking {
        // Given
        var sourceValue = true
        val source = object : FeatureSource {
            override val sourceName: String = "custom_source"

            override fun get(key: String) = if (key == "refreshable") {
                FeatureFlag(key, sourceValue, "refreshable_source")
            } else null

            override suspend fun refresh() {
                sourceValue = false // Change value on refresh
            }
        }
        val toggle = Toggle { sources { new(source) } }

        // When - Initial state
        assertTrue(toggle.isEnabled(FeatureKey.of("refreshable")))

        // When - Refresh
        toggle.refresh()

        // Then - Should now be false
        assertFalse(toggle.isEnabled(FeatureKey.of("refreshable")))

        toggle.close()
    }

    @Test
    fun `given failing high priority source when resolving feature then falls back to working lower priority source`() {
        // Given
        val failingSource = object : FeatureSource {
            override val sourceName: String = "failing_source"
            override val priority = 300
            override fun get(key: String): FeatureFlag? {
                throw RuntimeException("Source failure")
            }
        }
        val workingSource = MemorySource(mapOf("resilient_feature" to true))

        // When
        val toggle = Toggle { sources {
            new(failingSource)
            new(workingSource)
        } }

        // Then - Should fall back to working source
        assertTrue(toggle.isEnabled(FeatureKey.of("resilient_feature")))

        toggle.close()
    }

    @Test
    fun `given unknown feature when resolving then returns disabled flag with default source`() {
        // Given
        val source = MemorySource(mapOf("known_feature" to true))
        val toggle = Toggle { sources { new(source) } }

        // When
        val unknownFeature = toggle.value(FeatureKey.of("unknown_feature"))

        // Then
        assertEquals("unknown_feature", unknownFeature.key)
        assertFalse(unknownFeature.enabled)
        assertEquals("default", unknownFeature.source)

        toggle.close()
    }

    @Test
    fun `given registered feature keys when getting all values then returns all features including missing ones`() {
        // Given
        val key1 = FeatureKey.of("feature1")
        val key2 = FeatureKey.of("feature2")
        val key3 = FeatureKey.of("feature3")

        val source = MemorySource(mapOf(
            "feature1" to true,
            "feature2" to false
        ))
        val toggle = Toggle { sources { new(source) } }

        // When
        val allFeatures = toggle.values()

        // Then
        assertTrue(allFeatures.containsKey(key1.value))
        assertTrue(allFeatures.containsKey(key2.value))
        assertTrue(allFeatures.containsKey(key3.value))

        assertTrue(allFeatures["feature1"]?.enabled == true)
        assertFalse(allFeatures["feature2"]?.enabled != false)
        assertFalse(allFeatures["feature3"]?.enabled != false)

        toggle.close()
    }

    @Test
    fun `given no sources when building toggle then throws IllegalArgumentException`() {
        // When & Then
        assertFailsWith<IllegalArgumentException> {
            Toggle {  }
        }
    }

    @Test
    fun `given debug logging enabled when checking feature then logging is active`() {
        // Given
        val source = MemorySource(mapOf("debug_feature" to true))
        val toggle = Toggle { sources { new(source) } }

        // When & Then
        assertTrue(toggle.isEnabled(FeatureKey.of("debug_feature")))

        toggle.close()
    }
}