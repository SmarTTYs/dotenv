package io.github.smarttys.dotenv.internal

import io.github.smarttys.dotenv.EnvMap
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.toKString
import platform.posix.SEEK_END
import platform.posix.environ
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread
import platform.posix.fseek
import platform.posix.ftell
import platform.posix.rewind
import platform.windows.byteVar

internal actual fun readFile(filePath: String): ByteArray? {
    val file = fopen(filePath, "r") ?: return null

    try {
        memScoped {
            fseek(file, 0, SEEK_END)
            val fileLen = ftell(file)
            rewind(file)

            val buffer = allocArray<byteVar>(fileLen)
            fread(buffer, fileLen.toULong(), 1, file)

            return buffer.readBytes(fileLen)
        }
    } finally {
        fclose(file)
    }
}

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
