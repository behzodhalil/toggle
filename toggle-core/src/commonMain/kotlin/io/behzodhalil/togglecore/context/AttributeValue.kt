package io.behzodhalil.togglecore.context

import kotlinx.serialization.Serializable

@Serializable
sealed interface AttributeValue {
    @Serializable
    data class StringValue(val value: String) : AttributeValue

    @Serializable
    data class DoubleValue(val value: Double) : AttributeValue

    @Serializable
    data class IntValue(val value: Int) : AttributeValue

    @Serializable
    data class ShortValue(val value: Short) : AttributeValue

    @Serializable
    data class LongValue(val value: Long) : AttributeValue

    @Serializable
    data class BooleanValue(val value: Boolean) : AttributeValue
}