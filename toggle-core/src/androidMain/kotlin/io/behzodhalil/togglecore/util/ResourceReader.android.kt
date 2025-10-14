package io.behzodhalil.togglecore.util

public class JvmResourceReader : ResourceReader {
    override fun readText(path: String): String {
        val normalizedPath = if (path.startsWith("/")) path else "/$path"

        val inputStream =
            JvmResourceReader::class.java.getResourceAsStream(normalizedPath)
                ?: throw IllegalArgumentException("Resource not found at path: $path")

        return inputStream.use {
            // Use a standard, non-default charset (UTF-8)
            it.readBytes().toString(Charsets.UTF_8)
        }
    }
}

public actual fun getResourceReader(): ResourceReader = JvmResourceReader()
