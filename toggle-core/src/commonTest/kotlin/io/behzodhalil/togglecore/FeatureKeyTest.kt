package io.behzodhalil.togglecore

import io.behzodhalil.togglecore.core.FeatureKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class FeatureKeyTest {

    @Test
    fun `given predefined feature key when accessing value then returns correct key string`() {
        // Given
        val key = FeatureKey.EXPERIMENTAL_API

        // When & Then
        assertEquals("experimental_api", key.value)
    }

    @Test
    fun `given new feature keys when created then they are registered in global registry`() {
        // Given
        val initialSize = FeatureKey.registry.size

        // When
        val key1 = FeatureKey.of("new_feature_1")
        val key2 = FeatureKey.of("new_feature_2")

        // Then
        assertTrue(FeatureKey.registry.contains(key1))
        assertTrue(FeatureKey.registry.contains(key2))
        assertEquals(initialSize + 2, FeatureKey.registry.size)
    }

    @Test
    fun `given duplicate feature key when created multiple times then only one instance is registered`() {
        // Given
        val initialSize = FeatureKey.registry.size

        // When
        val key1 = FeatureKey.of("duplicate_key")
        val key2 = FeatureKey.of("duplicate_key")

        // Then
        assertEquals(key1, key2)
        assertEquals(initialSize + 1, FeatureKey.registry.size)
    }

    @Test
    fun `given blank key string when creating feature key then throws IllegalArgumentException`() {
        // When & Then
        assertFailsWith<IllegalArgumentException> {
            FeatureKey.of("")
        }
    }

    @Test
    fun `given whitespace only key string when creating feature key then throws IllegalArgumentException`() {
        // When & Then
        assertFailsWith<IllegalArgumentException> {
            FeatureKey.of("   ")
        }
    }

    @Test
    fun `given two feature keys with same value when comparing then they are equal`() {
        // Given
        val key1 = FeatureKey.of("same_key")
        val key2 = FeatureKey.of("same_key")
        val key3 = FeatureKey.of("different_key")

        // When & Then
        assertEquals(key1, key2)
        assertTrue(key1 != key3)
        assertEquals(key1.hashCode(), key2.hashCode())
    }

    @Test
    fun `given feature key when calling toString then returns formatted representation`() {
        // Given
        val key = FeatureKey.of("test_feature")

        // When
        val stringRepresentation = key.toString()

        // Then
        assertEquals("FeatureKey(test_feature)", stringRepresentation)
    }

    @Test
    fun `given registered feature keys when getting all keys then returns complete registry`() {
        // Given
        val beforeCount = FeatureKey.registry.size

        // When
        val key1 = FeatureKey.of("registry_test_1")
        val key2 = FeatureKey.of("registry_test_2")
        val allKeys = FeatureKey.registry

        // Then
        assertTrue(allKeys.contains(key1))
        assertTrue(allKeys.contains(key2))
        assertEquals(beforeCount + 2, allKeys.size)
    }
}