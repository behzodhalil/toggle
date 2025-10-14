package io.behzodhalil.togglecore.util

/**
 * Contract for platform-specific loading of text resources (like YAML files).
 */
public interface ResourceReader {
    /**
     * Reads the entire content of a resource file into a String.
     * Throws an exception if the resource is not found or cannot be read.
     * @param path The path to the resource (e.g., "my_config.yaml" or "data/config.json")
     */
    public fun readText(path: String): String
}

/**
 * Factory function to get the platform-specific ResourceReader implementation.
 */
public expect fun getResourceReader(): ResourceReader
