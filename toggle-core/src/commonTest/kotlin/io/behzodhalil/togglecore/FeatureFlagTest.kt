package io.behzodhalil.togglecore

import io.behzodhalil.togglecore.core.FeatureFlag
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FeatureFlagTest {

    @Test
    fun `given all flag properties when creating feature flag then all fields are set correctly`() {
        // Given & When
        val featureFlag = FeatureFlag(
            key = "test_feature",
            enabled = true,
            source = "test",
            metadata = mapOf("version" to "1.0"),
            timestamp = 1234567890L
        )

        // Then
        assertEquals("test_feature", featureFlag.key)
        assertTrue(featureFlag.enabled)
        assertEquals("test", featureFlag.source)
        assertEquals("1.0", featureFlag.metadata["version"])
        assertEquals(1234567890L, featureFlag.timestamp)
    }

    @Test
    fun `given minimal properties when creating disabled flag then uses defaults`() {
        // Given & When
        val featureFlag = FeatureFlag(
            key = "disabled_feature",
            enabled = false,
            source = "config"
        )

        // Then
        assertEquals("disabled_feature", featureFlag.key)
        assertFalse(featureFlag.enabled)
        assertEquals("config", featureFlag.source)
        assertTrue(featureFlag.metadata.isEmpty())
        assertTrue(featureFlag.timestamp > 0)
    }

    @Test
    fun `given feature key when using disabled factory method then creates disabled flag with defaults`() {
        // When
        val featureFlag = FeatureFlag.disabled("missing_feature")

        // Then
        assertEquals("missing_feature", featureFlag.key)
        assertFalse(featureFlag.enabled)
        assertEquals("default", featureFlag.source)
        assertTrue(featureFlag.metadata.isEmpty())
    }

    @Test
    fun `given feature flag when serializing to JSON then can deserialize with same values`() {
        // Given
        val featureFlag = FeatureFlag(
            key = "serializable_feature",
            enabled = true,
            source = "json",
            metadata = mapOf("test" to "value")
        )

        // When
        val json = Json.encodeToString(FeatureFlag.serializer(), featureFlag)
        val deserialized = Json.decodeFromString(FeatureFlag.serializer(), json)

        // Then
        assertEquals(featureFlag.key, deserialized.key)
        assertEquals(featureFlag.enabled, deserialized.enabled)
        assertEquals(featureFlag.source, deserialized.source)
        assertEquals(featureFlag.metadata, deserialized.metadata)
    }

    @Test
    fun `given two feature flags with same properties when comparing then they are equal`() {
        // Given
        val featureFlag1 = FeatureFlag("test", true, "source1", mapOf("a" to "b"), 100L)
        val featureFlag2 = FeatureFlag("test", true, "source1", mapOf("a" to "b"), 100L)
        val featureFlag3 = FeatureFlag("test", false, "source1", mapOf("a" to "b"), 100L)

        // When & Then
        assertEquals(featureFlag1, featureFlag2)
        assertTrue(featureFlag1 != featureFlag3)
    }

    @Test
    fun `given feature flag when calling toString then contains key properties`() {
        // Given
        val featureFlag = FeatureFlag("test_feature", true, "memory")

        // When
        val string = featureFlag.toString()

        // Then
        assertTrue(string.contains("test_feature"))
        assertTrue(string.contains("true"))
        assertTrue(string.contains("memory"))
    }
}