package io.github.smarttys.dotenv.internal

import io.github.smarttys.dotenv.EnvMap
import kotlinx.cinterop.*
import platform.Foundation.*

@OptIn(ExperimentalForeignApi::class, UnsafeNumber::class)
internal actual fun readFile(filePath: String): ByteArray? {
    return NSString.stringWithContentsOfFile(filePath, NSUTF8StringEncoding, null)?.encodeToByteArray()
}

internal actual fun readEnvironmentMap(): EnvMap {
    val envMap = mutableMapOf<String, String>()
    NSProcessInfo.processInfo.environment().forEach { (key, value) ->
        envMap[key.toString()] = value.toString()
    }

    return envMap
}
