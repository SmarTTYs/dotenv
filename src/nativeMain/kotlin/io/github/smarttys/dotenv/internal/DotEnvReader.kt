package io.github.smarttys.dotenv.internal

/*
@OptIn(ExperimentalForeignApi::class, UnsafeNumber::class)
internal actual fun readFile(filePath: String): ByteArray? {
    val file = fopen(filePath, "r") ?: return null

    try {
        memScoped {
            fseek(file, 0, SEEK_END)
            @Suppress("RemoveRedundantCallsOfConversionMethods")
            val fileLen = ftell(file).toInt()
            rewind(file)

            val buffer = allocArray<ByteVar>(fileLen)
            fread(buffer, fileLen.toUInt(), 1u, file)

            return buffer.readBytes(fileLen)
        }
    } finally {
        fclose(file)
    }
}
*/
