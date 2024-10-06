package io.github.smarttys.dotenv.internal

import io.github.smarttys.dotenv.EnvMap

internal expect fun writeEnvToFile(filePath: String, input: String)

internal fun marshallAndWriteEnvToFile(envMap: EnvMap, filePath: String) {
    val content = marshall(envMap.entries)
    writeEnvToFile(filePath, content)
}

private fun marshall(envEntries: Collection<Map.Entry<String, String>>): String {
    return envEntries.joinToString(separator = "\n") { (key, value) ->
        "$key=${value.doubleQuoteEscape()}"
    }
}

private fun String.doubleQuoteEscape(): String = buildString(length shl 1) {
    for (char in this@doubleQuoteEscape) {
        val toReplace = when (char) {
            '\n' -> "\n"
            '\r' -> "\r"
            else -> char.toString()
        }

        append(toReplace)
    }
}

internal fun throwFileOpenException(filePath: String): Nothing {
    error("There was an exception while opening/creating output file $filePath!")
}
