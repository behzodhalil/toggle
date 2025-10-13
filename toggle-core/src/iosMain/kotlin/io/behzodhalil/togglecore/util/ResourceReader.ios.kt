package io.behzodhalil.togglecore.util

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSBundle
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile

public class NSResourceReader : ResourceReader {
    @OptIn(ExperimentalForeignApi::class)
    override fun readText(path: String): String {
        val bundle = NSBundle.mainBundle
        val resourcePath = bundle.pathForResource(path, ofType = null)
            ?: error("Resource not found: $path")
        return NSString.stringWithContentsOfFile(
            resourcePath,
            encoding = NSUTF8StringEncoding,
            error = null
        ) as String
    }
}

public actual fun getResourceReader(): ResourceReader = NSResourceReader()