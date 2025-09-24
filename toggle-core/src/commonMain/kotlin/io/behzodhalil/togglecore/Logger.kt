package io.behzodhalil.togglecore


interface ToggleLogger {
    fun log(message: String, throwable: Throwable? = null)
}
