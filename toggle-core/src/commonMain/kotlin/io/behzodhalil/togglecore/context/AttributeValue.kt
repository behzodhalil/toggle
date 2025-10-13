package io.behzodhalil.togglecore.context

import kotlinx.serialization.Serializable

@Serializable
public sealed interface AttributeValue {
    @Serializable
    public data class StringValue(val value: String) : AttributeValue

    @Serializable
    public data class DoubleValue(val value: Double) : AttributeValue

    @Serializable
    public data class IntValue(val value: Int) : AttributeValue

    @Serializable
    public data class ShortValue(val value: Short) : AttributeValue

    @Serializable
    public data class LongValue(val value: Long) : AttributeValue

    @Serializable
    public data class BooleanValue(val value: Boolean) : AttributeValue
}