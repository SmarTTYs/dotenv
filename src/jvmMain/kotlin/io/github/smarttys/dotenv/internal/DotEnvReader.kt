@file:JvmName("DotEnvReaderExtensions")

package io.github.smarttys.dotenv.internal

import java.nio.file.Path
import kotlin.io.path.isReadable
import kotlin.io.path.readBytes

internal actual fun readEnvironmentMap() = System.getenv() + System.getProperties().entries.associate {
    it.key.toString() to it.value.toString()
}

internal actual fun readFile(filePath: String): ByteArray? {
    val path = Path.of(filePath).takeIf(Path::isReadable) ?: return null
    return path.readBytes()
}
