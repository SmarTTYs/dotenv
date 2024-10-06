package io.github.smarttys.dotenv.internal

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.memScoped
import platform.posix.EOF
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fputs

@OptIn(ExperimentalForeignApi::class)
internal actual fun writeEnvToFile(filePath: String, input: String) {
    val file = fopen(filePath, "wb") ?: throwFileOpenException(filePath)
    try {
        memScoped {
            if (fputs(input, file) == EOF) {
                error("There was an error while writing into the target file!")
            }
        }
    } finally {
        fclose(file)
    }
}
