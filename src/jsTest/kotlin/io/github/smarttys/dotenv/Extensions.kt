package io.github.smarttys.dotenv

import io.github.smarttys.dotenv.internal.fs
import io.github.smarttys.dotenv.internal.get
import io.github.smarttys.dotenv.internal.process

private const val NODE_JS_SUFFIX = "/build/js/node_modules/mocha/bin"

actual fun readFromSystem(key: String): String? = process.env[key]
actual fun checkFileExistsAndRemove(filePath: String): Boolean {
    return if (fs.existsSync(filePath) as Boolean) {
        fs.unlinkSync(filePath)
        true
    } else {
        false
    }
}

actual fun extractTestFilePath(originalPath: String): String {
    val transformedPath = originalPath.removePrefix(".")
    val testInstancePath = js("require('path').dirname(require.main.filename)") as String

    val updatedInstancePath = testInstancePath.removeSuffix(NODE_JS_SUFFIX)
    return updatedInstancePath + transformedPath
}
