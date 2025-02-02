package io.github.smarttys.dotenv.internal

import io.github.smarttys.dotenv.EnvMap
import kotlinx.cinterop.*
import platform.posix.*

@OptIn(ExperimentalForeignApi::class)
internal actual fun readEnvironmentMap(): EnvMap {
    val environment = environ ?: return emptyMap()

    var index = 0
    val envMap = mutableMapOf<String, String>()
    while (true) {
        val keyValuePair = environment[index++] ?: break
        val (key, value) = keyValuePair.toKString().split("=", limit = 2)
        envMap[key] = value
    }

    return envMap
}

@OptIn(ExperimentalForeignApi::class, UnsafeNumber::class)
internal actual fun readFile(filePath: String): ByteArray? {
    val file = fopen(filePath, "r") ?: return null

    try {
        memScoped {
            fseek(file, 0, SEEK_END)
            val fileLen = ftell(file)
            rewind(file)

            val buffer = allocArray<ByteVar>(fileLen)
            fread(buffer, fileLen.toUInt(), 1u, file)

            return buffer.readBytes(fileLen)
        }
    } finally {
        fclose(file)
    }
}
