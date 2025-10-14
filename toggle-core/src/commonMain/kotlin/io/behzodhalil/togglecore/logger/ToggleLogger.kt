package io.behzodhalil.togglecore.logger

public interface ToggleLogger {
    public fun log(
        message: String,
        throwable: Throwable? = null,
    )
}
