@file:JvmName("DotEnvReaderExtensions")

package io.github.smarttys.dotenv.internal

import io.github.smarttys.dotenv.EnvMap
import java.nio.file.Path
import kotlin.io.path.isReadable
import kotlin.io.path.readBytes

internal actual fun readEnvironmentMap(): EnvMap {
    val envMap = System.getenv()
    val propertiesMap = System.getProperties().entries.associate {
        it.key.toString() to it.value.toString()
    }

    return envMap + propertiesMap
}

internal actual fun readFile(filePath: String): ByteArray? {
    val path = Path.of(filePath).takeIf(Path::isReadable) ?: return null
    return path.readBytes()
}
