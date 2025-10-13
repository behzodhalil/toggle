package io.behzodhalil.togglecore.logger

import io.behzodhalil.togglecore.core.ToggleInternal

@ToggleInternal
public class LoggingScope {
    private var logger: ToggleLogger? = null

    /**
     * Enable platform-specific debug logging
     */
    public fun logging() {
        this.logger = createPlatformLogger()
    }

    /**
     * Use custom logger
     */
    public fun custom(logger: ToggleLogger) {
        this.logger = logger
    }

    /**
     * Disable logging
     */
    public fun none() {
        this.logger = null
    }

    internal fun build(): ToggleLogger? = logger
}