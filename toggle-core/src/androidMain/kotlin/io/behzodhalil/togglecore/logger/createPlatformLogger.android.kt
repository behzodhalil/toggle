package io.behzodhalil.togglecore.logger

import android.util.Log

public actual fun createPlatformLogger(): ToggleLogger = object : ToggleLogger {

    override fun log(message: String, throwable: Throwable?) {
        if (throwable != null) {
            Log.e("Toggle", message, throwable)
        } else {
            Log.d("Toggle", message)
        }
    }
}