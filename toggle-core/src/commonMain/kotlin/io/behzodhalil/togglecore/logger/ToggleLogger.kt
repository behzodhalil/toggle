package io.behzodhalil.togglecore.logger


interface ToggleLogger {
    fun log(message: String, throwable: Throwable? = null)
}
