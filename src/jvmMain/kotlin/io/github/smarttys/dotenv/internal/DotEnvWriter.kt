@file:JvmName("DotEnvWriterImpl")

package io.github.smarttys.dotenv.internal

import kotlin.io.path.Path
import kotlin.io.path.createFile
import kotlin.io.path.isWritable
import kotlin.io.path.writeText

internal actual fun writeEnvToFile(filePath: String, input: String) {
    val path = Path(filePath).createFile()
    if (path.isWritable()) {
        path.writeText(input)
    } else {
        throwFileOpenException(filePath)
    }
}
