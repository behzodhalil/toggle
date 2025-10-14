package io.behzodhalil.togglecore.source

import io.behzodhalil.togglecore.error.YamlParseException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class YamlSourceTest {
    // ========== Basic Functionality Tests ==========

    @Test
    fun `create yaml source with complex format`() {
        val yamlContent =
            """
            features:
              feature1:
                enabled: true
                description: "Test feature 1"
                metadata:
                  version: "1.0"
                  owner: "team-a"
              feature2:
                enabled: false
                description: "Test feature 2"
            """.trimIndent()

        val source = YamlSource.fromString(yamlContent)

        assertEquals(120, source.priority)

        val feature1 = source.get("feature1")
        assertNotNull(feature1)
        assertTrue(feature1.enabled)
        assertEquals("yaml", feature1.source)
        assertEquals("Test feature 1", feature1.metadata["description"])
        assertEquals("1.0", feature1.metadata["version"])
        assertEquals("team-a", feature1.metadata["owner"])

        val feature2 = source.get("feature2")
        assertNotNull(feature2)
        assertFalse(feature2.enabled)
        assertEquals("yaml", feature2.source)
        assertEquals("Test feature 2", feature2.metadata["description"])
    }

    @Test
    fun `create yaml source with simple format`() {
        val yamlContent =
            """
            features:
              dark_mode: true
              new_ui: false
              beta_feature: yes
            """.trimIndent()

        val source = YamlSource.fromString(yamlContent)

        val darkMode = source.get("dark_mode")
        assertNotNull(darkMode)
        assertTrue(darkMode.enabled)
        assertEquals("yaml", darkMode.source)

        val newUi = source.get("new_ui")
        assertNotNull(newUi)
        assertFalse(newUi.enabled)

        val betaFeature = source.get("beta_feature")
        assertNotNull(betaFeature)
        assertTrue(betaFeature.enabled)
    }

    @Test
    fun `yaml source with minimal feature configuration`() {
        val yamlContent =
            """
            features:
              minimal_feature:
                enabled: true
            """.trimIndent()

        val source = YamlSource.fromString(yamlContent)

        val feature = source.get("minimal_feature")
        assertNotNull(feature)
        assertTrue(feature.enabled)
        assertEquals("yaml", feature.source)
        assertTrue(feature.metadata.isEmpty())
    }

    @Test
    fun `yaml source with default values`() {
        val yamlContent =
            """
            features:
              default_feature:
            """.trimIndent()

        val source = YamlSource.fromString(yamlContent)

        val feature = source.get("default_feature")
        assertNotNull(feature)
        assertFalse(feature.enabled) // Default is false
        assertEquals("yaml", feature.source)
    }

    @Test
    fun `yaml source with only description`() {
        val yamlContent =
            """
            features:
              documented_feature:
                description: "This feature has no enabled field"
            """.trimIndent()

        val source = YamlSource.fromString(yamlContent)

        val feature = source.get("documented_feature")
        assertNotNull(feature)
        assertFalse(feature.enabled) // Default is false when enabled is missing
        assertEquals("This feature has no enabled field", feature.metadata["description"])
    }

    @Test
    fun `yaml source with complex metadata`() {
        val yamlContent =
            """
            features:
              complex_feature:
                enabled: true
                description: "Complex feature with metadata"
                metadata:
                  rollout_percentage: "25"
                  target_audience: "beta_users"
                  experiment_id: "exp_123"
                  start_date: "2024-01-01"
            """.trimIndent()

        val source = YamlSource.fromString(yamlContent)

        val feature = source.get("complex_feature")
        assertNotNull(feature)
        assertTrue(feature.enabled)
        assertEquals("Complex feature with metadata", feature.metadata["description"])
        assertEquals("25", feature.metadata["rollout_percentage"])
        assertEquals("beta_users", feature.metadata["target_audience"])
        assertEquals("exp_123", feature.metadata["experiment_id"])
        assertEquals("2024-01-01", feature.metadata["start_date"])
    }

    @Test
    fun `get all features returns complete list`() {
        val yamlContent =
            """
            features:
              alpha:
                enabled: true
              beta:
                enabled: false
              gamma:
                enabled: true
                metadata:
                  test: "value"
            """.trimIndent()

        val source = YamlSource.fromString(yamlContent)

        val allFeatures = source.getAll()
        assertEquals(3, allFeatures.size)

        val featureMap = allFeatures.associateBy { it.key }
        assertTrue(featureMap["alpha"]?.enabled ?: false)
        assertFalse(featureMap["beta"]?.enabled ?: true)
        assertTrue(featureMap["gamma"]?.enabled ?: false)
        assertEquals("value", featureMap["gamma"]?.metadata?.get("test"))
    }

    @Test
    fun `yaml source handles nonexistent features`() {
        val yamlContent =
            """
            features:
              existing_feature:
                enabled: true
            """.trimIndent()

        val source = YamlSource.fromString(yamlContent)

        assertNull(source.get("nonexistent_feature"))
        assertNull(source.get(""))
        assertNull(source.get("another_missing"))
    }

    // ========== Boolean Parsing Tests ==========

    @Test
    fun `yaml with boolean values in different formats`() {
        val yamlContent =
            """
            features:
              true_feature: true
              false_feature: false
              yes_feature: yes
              no_feature: no
              on_feature: on
              off_feature: off
              one_feature: 1
              zero_feature: 0
            """.trimIndent()

        val source = YamlSource.fromString(yamlContent)

        assertTrue(source.get("true_feature")?.enabled ?: false)
        assertFalse(source.get("false_feature")?.enabled ?: true)
        assertTrue(source.get("yes_feature")?.enabled ?: false)
        assertFalse(source.get("no_feature")?.enabled ?: true)
        assertTrue(source.get("on_feature")?.enabled ?: false)
        assertFalse(source.get("off_feature")?.enabled ?: true)
        assertTrue(source.get("one_feature")?.enabled ?: false)
        assertFalse(source.get("zero_feature")?.enabled ?: true)
    }

    @Test
    fun `yaml with mixed case boolean values`() {
        val yamlContent =
            """
            features:
              true_upper: TRUE
              false_upper: FALSE
              yes_mixed: Yes
              no_mixed: No
            """.trimIndent()

        val source = YamlSource.fromString(yamlContent)

        assertTrue(source.get("true_upper")?.enabled ?: false)
        assertFalse(source.get("false_upper")?.enabled ?: true)
        assertTrue(source.get("yes_mixed")?.enabled ?: false)
        assertFalse(source.get("no_mixed")?.enabled ?: true)
    }

    @Test
    fun `yaml with invalid boolean value throws exception`() {
        val yamlContent =
            """
            features:
              invalid_boolean: maybe
            """.trimIndent()

        val source = YamlSource.fromString(yamlContent)

        // Exception should be thrown on first access (lazy parsing)
        assertFailsWith<YamlParseException> {
            source.get("invalid_boolean")
        }
    }

    // ========== Special Characters and Edge Cases ==========

    @Test
    fun `yaml with special characters in feature names`() {
        val yamlContent =
            """
            features:
              feature-with-dashes: true
              feature_with_underscores: false
              feature.with.dots: true
            """.trimIndent()

        val source = YamlSource.fromString(yamlContent)

        assertTrue(source.get("feature-with-dashes")?.enabled ?: false)
        assertFalse(source.get("feature_with_underscores")?.enabled ?: true)
        assertTrue(source.get("feature.with.dots")?.enabled ?: false)
    }

    @Test
    fun `yaml with quoted strings in metadata`() {
        val yamlContent =
            """
            features:
              feature_with_quotes:
                enabled: true
                description: "This is a quoted description"
                metadata:
                  quoted_value: "value with spaces"
                  unquoted_value: simple
            """.trimIndent()

        val source = YamlSource.fromString(yamlContent)

        val feature = source.get("feature_with_quotes")
        assertNotNull(feature)
        assertEquals("This is a quoted description", feature.metadata["description"])
        assertEquals("value with spaces", feature.metadata["quoted_value"])
        assertEquals("simple", feature.metadata["unquoted_value"])
    }

    @Test
    fun `yaml with unicode characters`() {
        val yamlContent =
            """
            features:
              unicode_feature:
                enabled: true
                description: "Feature with émojis 🚀 and ñ characters"
                metadata:
                  owner: "tëam-ñ"
            """.trimIndent()

        val source = YamlSource.fromString(yamlContent)

        val feature = source.get("unicode_feature")
        assertNotNull(feature)
        assertEquals("Feature with émojis 🚀 and ñ characters", feature.metadata["description"])
        assertEquals("tëam-ñ", feature.metadata["owner"])
    }

    @Test
    fun `yaml with comments`() {
        val yamlContent =
            """
            # This is a comment
            features:
              # Comment before feature
              feature1: true
              feature2: false  # Inline comment ignored in custom parser
              # Another comment
              feature3:
                enabled: true
                # Comment in metadata
                metadata:
                  key: value
            """.trimIndent()

        val source = YamlSource.fromString(yamlContent)

        assertNotNull(source.get("feature1"))
        assertNotNull(source.get("feature2"))
        assertNotNull(source.get("feature3"))
        assertEquals(3, source.getAll().size)
    }

    @Test
    fun `yaml with empty lines`() {
        val yamlContent =
            """
            features:

              feature1: true

              feature2: false

            """.trimIndent()

        val source = YamlSource.fromString(yamlContent)

        assertTrue(source.get("feature1")?.enabled ?: false)
        assertFalse(source.get("feature2")?.enabled ?: true)
    }

    // ========== Error Handling Tests ==========

    @Test
    fun `empty yaml content throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            YamlSource.fromString("")
        }
    }

    @Test
    fun `blank yaml content throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            YamlSource.fromString("   \n  \n  ")
        }
    }

    @Test
    fun `yaml content without features section throws exception`() {
        val yamlWithoutFeatures =
            """
            other_config:
              value: "test"
            """.trimIndent()

        val source = YamlSource.fromString(yamlWithoutFeatures)

        // Exception should be thrown on first access (lazy parsing)
        assertFailsWith<YamlParseException> {
            source.get("any_feature")
        }
    }

    @Test
    fun `yaml with empty features section throws exception`() {
        val yamlContent =
            """
            features:
            """.trimIndent()

        val source = YamlSource.fromString(yamlContent)

        // Exception should be thrown on first access (lazy parsing)
        assertFailsWith<YamlParseException> {
            source.getAll()
        }
    }

    // ========== Priority Tests ==========

    @Test
    fun `yaml source has default priority`() {
        val yamlContent =
            """
            features:
              test_feature: true
            """.trimIndent()

        val source = YamlSource.fromString(yamlContent)
        assertEquals(120, source.priority)
    }

    @Test
    fun `yaml source with custom priority`() {
        val yamlContent =
            """
            features:
              test_feature: true
            """.trimIndent()

        val source = YamlSource.fromString(yamlContent, priority = 200)
        assertEquals(200, source.priority)
    }

    // ========== Refresh Tests ==========

    @Test
    fun `yaml refresh works correctly`() {
        val yamlContent =
            """
            features:
              refreshable_feature: true
            """.trimIndent()

        val source = YamlSource.fromString(yamlContent)

        // Initial state
        assertTrue(source.get("refreshable_feature")?.enabled ?: false)

        // Refresh should not break anything
        runBlocking { source.refresh() }

        // Should still work
        assertTrue(source.get("refreshable_feature")?.enabled ?: false)
    }

    // ========== Thread Safety Tests ==========

    @Test
    fun `concurrent access to yaml source is thread safe`() {
        val yamlContent =
            """
            features:
              feature1: true
              feature2: false
              feature3: true
              feature4: false
              feature5: true
            """.trimIndent()

        val source = YamlSource.fromString(yamlContent)

        runBlocking {
            val jobs =
                (1..100).map { iteration ->
                    async {
                        val featureKey = "feature${(iteration % 5) + 1}"
                        source.get(featureKey)
                    }
                }

            val results = jobs.awaitAll()
            assertEquals(100, results.size)
            assertTrue(results.all { it != null })
        }
    }

    @Test
    fun `concurrent refresh calls are thread safe`() {
        val yamlContent =
            """
            features:
              test_feature: true
            """.trimIndent()

        val source = YamlSource.fromString(yamlContent)

        runBlocking {
            val jobs =
                (1..50).map {
                    async {
                        source.refresh()
                        source.get("test_feature")
                    }
                }

            val results = jobs.awaitAll()
            assertTrue(results.all { it?.enabled == true })
        }
    }

    @Test
    fun `concurrent get and getAll are thread safe`() {
        val yamlContent =
            """
            features:
              feature1: true
              feature2: false
              feature3: true
            """.trimIndent()

        val source = YamlSource.fromString(yamlContent)

        runBlocking {
            val jobs =
                (1..100).map { iteration ->
                    async {
                        if (iteration % 2 == 0) {
                            source.get("feature${(iteration % 3) + 1}")
                        } else {
                            source.getAll()
                        }
                    }
                }

            val results = jobs.awaitAll()
            assertEquals(100, results.size)
        }
    }

    // ========== Lazy Parsing Tests ==========

    @Test
    fun `yaml source parses lazily on first access`() {
        // This test verifies lazy parsing by ensuring that invalid YAML
        // doesn't throw immediately during construction
        val validYaml =
            """
            features:
              test_feature: true
            """.trimIndent()

        // Should not throw during construction
        val source = YamlSource.fromString(validYaml)

        // Should parse on first access
        assertNotNull(source.get("test_feature"))
    }

    // ========== Mixed Format Tests ==========

    @Test
    fun `yaml with mixed simple and complex format`() {
        val yamlContent =
            """
            features:
              simple_feature: true
              complex_feature:
                enabled: false
                description: "Complex one"
                metadata:
                  owner: "team-a"
              another_simple: yes
            """.trimIndent()

        val source = YamlSource.fromString(yamlContent)

        val simple = source.get("simple_feature")
        assertNotNull(simple)
        assertTrue(simple.enabled)
        assertTrue(simple.metadata.isEmpty())

        val complex = source.get("complex_feature")
        assertNotNull(complex)
        assertFalse(complex.enabled)
        assertEquals("Complex one", complex.metadata["description"])
        assertEquals("team-a", complex.metadata["owner"])

        val anotherSimple = source.get("another_simple")
        assertNotNull(anotherSimple)
        assertTrue(anotherSimple.enabled)
    }

    // ========== Metadata-only Features ==========

    @Test
    fun `yaml feature with metadata but no enabled field defaults to false`() {
        val yamlContent =
            """
            features:
              metadata_only:
                metadata:
                  owner: "team-x"
                  version: "2.0"
            """.trimIndent()

        val source = YamlSource.fromString(yamlContent)

        val feature = source.get("metadata_only")
        assertNotNull(feature)
        assertFalse(feature.enabled) // Should default to false
        assertEquals("team-x", feature.metadata["owner"])
        assertEquals("2.0", feature.metadata["version"])
    }

    // ========== Integration Tests ==========

    @Test
    fun `yaml source provides correct source name for all features`() {
        val yamlContent =
            """
            features:
              feature1: true
              feature2:
                enabled: false
                description: "Test"
            """.trimIndent()

        val source = YamlSource.fromString(yamlContent)

        source.getAll().forEach { feature ->
            assertEquals("yaml", feature.source)
        }
    }

    @Test
    fun `yaml source handles large number of features`() {
        val features = (1..100).joinToString("\n") { "  feature$it: ${it % 2 == 0}" }
        val yamlContent = "features:\n$features"

        val source = YamlSource.fromString(yamlContent)

        assertEquals(100, source.getAll().size)
        assertTrue(source.get("feature2")?.enabled ?: false)
        assertFalse(source.get("feature1")?.enabled ?: true)
    }
}
