package io.behzodhalil.togglecore.logger

import io.behzodhalil.togglecore.core.ToggleInternal

@ToggleInternal
class LoggingScope {
    private var logger: ToggleLogger? = null

    /**
     * Enable platform-specific debug logging
     */
    fun logging() {
        this.logger = createPlatformLogger()
    }

    /**
     * Use custom logger
     */
    fun custom(logger: ToggleLogger) {
        this.logger = logger
    }

    /**
     * Disable logging
     */
    fun none() {
        this.logger = null
    }

    internal fun build(): ToggleLogger? = logger
}