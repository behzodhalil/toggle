package io.behzodhalil.togglecore


import io.behzodhalil.togglecore.core.FeatureFlag
import io.behzodhalil.togglecore.context.AttributeValue
import io.behzodhalil.togglecore.context.ToggleContext
import io.behzodhalil.togglecore.context.getBooleanAttribute
import io.behzodhalil.togglecore.evaluator.NoOpRuleEvaluator
import io.behzodhalil.togglecore.evaluator.RuleEvaluator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FeatureEvaluatorTest {

    @Test
    fun `given default evaluator when evaluating feature then returns feature unchanged`() {
        // Given
        val evaluator = NoOpRuleEvaluator.INSTANCE
        val context = ToggleContext(userId = "test")
        val featureFlag = FeatureFlag("test_feature", true, "source")

        // When
        val result = evaluator.evaluate(featureFlag, context)

        // Then
        assertEquals(featureFlag, result)
    }

    @Test
    fun `given default evaluator and disabled feature when evaluating then returns disabled feature unchanged`() {
        // Given
        val evaluator = NoOpRuleEvaluator.INSTANCE
        val context = ToggleContext()
        val featureFlag = FeatureFlag("disabled_feature", false, "source")

        // When
        val result = evaluator.evaluate(featureFlag, context)

        // Then
        assertEquals(featureFlag, result)
        assertFalse(result.enabled)
    }

    @Test
    fun `given custom evaluator with user role check when evaluating then modifies state based on role`() {
        // Given
        val evaluator = object : RuleEvaluator {
            override fun evaluate(flag: FeatureFlag, context: ToggleContext): FeatureFlag {
                return if (context.userId == "admin") {
                    flag.copy(enabled = true)
                } else {
                    flag.copy(enabled = false)
                }
            }
        }
        val adminContext = ToggleContext(userId = "admin")
        val userContext = ToggleContext(userId = "user")
        val featureFlag = FeatureFlag("admin_feature", false, "source")

        // When
        val adminResult = evaluator.evaluate(featureFlag, adminContext)
        val userResult = evaluator.evaluate(featureFlag, userContext)

        // Then
        assertTrue(adminResult.enabled)
        assertFalse(userResult.enabled)
    }

    @Test
    fun `given evaluator with country whitelist when evaluating then enables only for allowed countries`() {
        // Given
        val evaluator = object : RuleEvaluator {
            override fun evaluate(flag: FeatureFlag, context: ToggleContext): FeatureFlag {
                val allowedCountries = setOf("US", "CA", "UK")
                return if (context.country in allowedCountries) {
                    flag
                } else {
                    flag.copy(enabled = false)
                }
            }
        }
        val usContext = ToggleContext(country = "US")
        val deContext = ToggleContext(country = "DE")
        val featureFlag = FeatureFlag("geo_feature", true, "source")

        // When
        val usResult = evaluator.evaluate(featureFlag, usContext)
        val deResult = evaluator.evaluate(featureFlag, deContext)

        // Then
        assertTrue(usResult.enabled)
        assertFalse(deResult.enabled)
    }

    @Test
    fun `given evaluator with version check when evaluating then enables only for minimum version`() {
        // Given
        val evaluator = object : RuleEvaluator {
            override fun evaluate(flag: FeatureFlag, context: ToggleContext): FeatureFlag {
                val minVersion = "2.0.0"
                val currentVersion = context.appVersion

                return if (currentVersion != null && currentVersion >= minVersion) {
                    flag
                } else {
                    flag.copy(enabled = false)
                }
            }
        }
        val newVersionContext = ToggleContext(appVersion = "2.1.0")
        val oldVersionContext = ToggleContext(appVersion = "1.9.0")
        val noVersionContext = ToggleContext()
        val featureFlag = FeatureFlag("version_feature", true, "source")

        // When
        val newResult = evaluator.evaluate(featureFlag, newVersionContext)
        val oldResult = evaluator.evaluate(featureFlag, oldVersionContext)
        val noVersionResult = evaluator.evaluate(featureFlag, noVersionContext)

        // Then
        assertTrue(newResult.enabled)
        assertFalse(oldResult.enabled)
        assertFalse(noVersionResult.enabled)
    }

    @Test
    fun `given evaluator with custom attribute check when evaluating then uses attribute value`() {
        // Given
        val evaluator = object : RuleEvaluator {
            override fun evaluate(flag: FeatureFlag, context: ToggleContext): FeatureFlag {
                val isBetaUser = context.getBooleanAttribute("betaUser")

                return if (isBetaUser) {
                    flag
                } else {
                    flag.copy(enabled = false)
                }
            }
        }
        val betaContext =
            ToggleContext(attributes = mapOf("betaUser" to AttributeValue.BooleanValue(true)))
        val regularContext =
            ToggleContext(attributes = mapOf("betaUser" to AttributeValue.BooleanValue(false)))
        val noAttributeContext = ToggleContext()
        val featureFlag = FeatureFlag("beta_feature", true, "source")

        // When
        val betaResult = evaluator.evaluate(featureFlag, betaContext)
        val regularResult = evaluator.evaluate(featureFlag, regularContext)
        val noAttributeResult = evaluator.evaluate(featureFlag, noAttributeContext)

        // Then
        assertTrue(betaResult.enabled)
        assertFalse(regularResult.enabled)
        assertFalse(noAttributeResult.enabled)
    }
}