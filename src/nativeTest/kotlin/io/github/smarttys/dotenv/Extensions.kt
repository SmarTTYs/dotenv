package io.github.smarttys.dotenv

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.getenv
import platform.posix.remove

@OptIn(ExperimentalForeignApi::class)
actual fun readFromSystem(key: String): String? = getenv(key)?.toKString()
actual fun checkFileExistsAndRemove(filePath: String): Boolean = remove(filePath) != -1

actual fun extractTestFilePath(originalPath: String): String = originalPath
