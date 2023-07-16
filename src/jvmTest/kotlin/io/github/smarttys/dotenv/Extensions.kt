@file:JvmName("TestExtensions")

package io.github.smarttys.dotenv

import java.io.File

actual fun readFromSystem(key: String): String? = System.getProperty(key)

actual fun checkFileExistsAndRemove(filePath: String): Boolean {
    val file = File(filePath).takeIf(File::exists) ?: return false
    return file.delete()
}

actual fun extractTestFilePath(originalPath: String): String = originalPath
