package io.behzodhalil.togglecore

import io.behzodhalil.togglecore.context.AttributeValue
import io.behzodhalil.togglecore.context.ToggleContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FeatureContextTest {
    @Test
    fun `given no parameters when creating context then all fields are null or empty`() {
        // When
        val context = ToggleContext()

        // Then
        assertNull(context.userId)
        assertNull(context.country)
        assertNull(context.language)
        assertNull(context.appVersion)
        assertNull(context.deviceId)
        assertTrue(context.attributes.isEmpty())
    }

    @Test
    fun `given all parameters when creating context then all fields are set correctly`() {
        // Given
        val attributes =
            mapOf(
                "custom" to AttributeValue.StringValue("value"),
                "number" to AttributeValue.IntValue(42),
            )

        // When
        val context =
            ToggleContext(
                userId = "user123",
                country = "US",
                language = "en",
                appVersion = "1.0.0",
                deviceId = "device456",
                attributes = attributes,
            )

        // Then
        assertEquals("user123", context.userId)
        assertEquals("US", context.country)
        assertEquals("en", context.language)
        assertEquals("1.0.0", context.appVersion)
        assertEquals("device456", context.deviceId)
        assertEquals(attributes, context.attributes)
    }

    @Test
    fun `given partial parameters when creating context then only provided fields are set`() {
        // When
        val context =
            ToggleContext(
                userId = "user789",
                country = "CA",
            )

        // Then
        assertEquals("user789", context.userId)
        assertEquals("CA", context.country)
        assertNull(context.language)
        assertNull(context.appVersion)
        assertNull(context.deviceId)
        assertTrue(context.attributes.isEmpty())
    }

    @Test
    fun `given only custom attributes when creating context then standard fields are null`() {
        // Given
        val attributes =
            mapOf(
                "platform" to AttributeValue.StringValue("android"),
                "debug" to AttributeValue.BooleanValue(true),
                "betaUser" to AttributeValue.BooleanValue(false),
            )

        // When
        val context = ToggleContext(attributes = attributes)

        // Then
        assertNull(context.userId)
        assertEquals(attributes, context.attributes)
        assertEquals(AttributeValue.StringValue("android"), context.attributes["platform"])
        assertEquals(AttributeValue.BooleanValue(true), context.attributes["debug"])
        assertEquals(AttributeValue.BooleanValue(false), context.attributes["betaUser"])
    }

    @Test
    fun `given empty attributes map when creating context then attributes remain empty`() {
        // When
        val context =
            ToggleContext(
                userId = "user",
                attributes = emptyMap(),
            )

        // Then
        assertEquals("user", context.userId)
        assertTrue(context.attributes.isEmpty())
    }
}
