package io.behzodhalil.togglecore.logger

import platform.Foundation.NSLog

actual fun createPlatformLogger(): ToggleLogger = object : ToggleLogger {

    override fun log(message: String, throwable: Throwable?) {
        val logMessage = if (throwable != null) {
            "Toggle: $message - Error: ${throwable.message}"
        } else {
            "Toggle: $message"
        }
        NSLog(logMessage)
    }
}