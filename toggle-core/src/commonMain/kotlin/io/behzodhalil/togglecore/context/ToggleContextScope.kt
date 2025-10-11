package io.behzodhalil.togglecore.context

import io.behzodhalil.togglecore.core.ToggleInternal

@ToggleInternal
class ToggleContextScope {
    private var userId: String? = null
    private var country: String? = null
    private var language: String? = null
    private var appVersion: String? = null
    private var deviceId: String? = null
    private val attributes = mutableMapOf<String, AttributeValue>()

    /**
     * Set user ID
     */
    fun userId(userId: String) {
        this.userId = userId
    }

    /**
     * Set country
     */
    fun country(country: String) {
        this.country = country
    }

    /**
     * Set language
     */
    fun language(language: String) {
        this.language = language
    }

    /**
     * Set app version
     */
    fun appVersion(version: String) {
        this.appVersion = version
    }

    /**
     * Set device ID
     */
    fun deviceId(deviceId: String) {
        this.deviceId = deviceId
    }

    /**
     * Add custom attribute
     */
    fun attribute(key: String, value: AttributeValue) {
        attributes[key] = value
    }

    /**
     * Add multiple attributes
     */
    fun attributes(vararg pairs: Pair<String, AttributeValue>) {
        attributes.putAll(pairs)
    }

    internal fun build(): ToggleContext {
        return ToggleContext(
            userId = userId,
            country = country,
            language = language,
            appVersion = appVersion,
            deviceId = deviceId,
            attributes = attributes
        )
    }
}