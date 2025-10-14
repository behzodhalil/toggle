package io.behzodhalil.togglecore.source

import io.behzodhalil.togglecore.core.FeatureKey
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MemorySourceTest {
    @Test
    fun `create empty memory source`() {
        val source = MemorySource()

        assertEquals(200, source.priority)
        assertNull(source.get("nonexistent"))
        assertTrue(source.getAll().isEmpty())
    }

    @Test
    fun `create memory source with initial features`() {
        val features =
            mapOf(
                "feature1" to true,
                "feature2" to false,
            )
        val source = MemorySource(features)

        val feature1 = source.get("feature1")
        val feature2 = source.get("feature2")

        assertEquals("feature1", feature1?.key)
        assertTrue(feature1?.enabled ?: false)
        assertEquals("memory", feature1?.source)

        assertEquals("feature2", feature2?.key)
        assertFalse(feature2?.enabled ?: true)
        assertEquals("memory", feature2?.source)
    }

    @Test
    fun `set feature by string key`() {
        val source = MemorySource()

        source.setFeature("new_feature", true)
        val feature = source.get("new_feature")

        assertEquals("new_feature", feature?.key)
        assertTrue(feature?.enabled ?: false)
        assertEquals("memory", feature?.source)
    }

    @Test
    fun `set feature by FeatureKey`() {
        val source = MemorySource()
        val featureKey = FeatureKey.of("typed_feature")

        source.setFeature(featureKey, false)
        val feature = source.get("typed_feature")

        assertEquals("typed_feature", feature?.key)
        assertFalse(feature?.enabled ?: true)
    }

    @Test
    fun `update existing feature`() {
        val source = MemorySource(mapOf("existing" to false))

        source.setFeature("existing", true)
        val feature = source.get("existing")

        assertTrue(feature?.enabled ?: false)
    }

    @Test
    fun `remove feature`() {
        val source = MemorySource(mapOf("removable" to true))

        val beforeRemoval = source.get("removable")
        assertTrue(beforeRemoval?.enabled ?: false)

        source.removeFeature("removable")
        val afterRemoval = source.get("removable")

        assertNull(afterRemoval)
    }

    @Test
    fun `clear all features`() {
        val source =
            MemorySource(
                mapOf(
                    "feature1" to true,
                    "feature2" to false,
                    "feature3" to true,
                ),
            )

        assertEquals(3, source.getAll().size)

        source.clear()

        assertTrue(source.getAll().isEmpty())
        assertNull(source.get("feature1"))
        assertNull(source.get("feature2"))
        assertNull(source.get("feature3"))
    }

    @Test
    fun `get all features returns correct list`() {
        val initialFeatures =
            mapOf(
                "alpha" to true,
                "beta" to false,
                "gamma" to true,
            )
        val source = MemorySource(initialFeatures)

        val allFeatures = source.getAll()

        assertEquals(3, allFeatures.size)

        val alphaFeature = allFeatures.find { it.key == "alpha" }
        val betaFeature = allFeatures.find { it.key == "beta" }
        val gammaFeature = allFeatures.find { it.key == "gamma" }

        assertTrue(alphaFeature?.enabled ?: false)
        assertFalse(betaFeature?.enabled ?: true)
        assertTrue(gammaFeature?.enabled ?: false)

        allFeatures.forEach { feature ->
            assertEquals("memory", feature.source)
        }
    }

    @Test
    fun `get nonexistent feature returns null`() {
        val source = MemorySource(mapOf("existing" to true))

        assertNull(source.get("nonexistent"))
        assertNull(source.get(""))
        assertNull(source.get("another_missing"))
    }

    @Test
    fun `memory source priority is correct`() {
        val source = MemorySource()
        assertEquals(200, source.priority)
    }

    @Test
    fun `refresh does nothing for memory source`() {
        val source = MemorySource(mapOf("test" to true))

        // Should not throw and should not change anything
        runBlocking { source.refresh() }

        val feature = source.get("test")
        assertTrue(feature?.enabled ?: false)
    }
}
