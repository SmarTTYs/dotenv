package io.github.smarttys.dotenv.internal

import io.github.smarttys.dotenv.DotEnv
import io.github.smarttys.dotenv.EnvMap
import io.github.smarttys.dotenv.exception.DotEnvException

internal expect fun readFile(filePath: String): ByteArray?
internal expect fun readEnvironmentMap(): EnvMap

internal fun readInputFile(file: String, ignoreMissingFile: Boolean): ByteArray? {
    return readFile(file) ?: if (ignoreMissingFile) {
        null
    } else throwMissingFileException(file)
}

internal fun getExpandedFilePaths(directory: String, files: Set<String>, systemEnvMap: EnvMap): List<String> {
    return files.map {
        val parsedFileName = it.removePrefix("/")
        val expandedPath = expandVariables("FilePath", directory, false, systemEnvMap)
        val expandedFileName = expandVariables("FileName", parsedFileName, false, systemEnvMap)

        expandedPath + expandedFileName
    }.ifEmpty(DotEnv::DEFAULT_FILE_LIST)
}

private fun throwMissingFileException(fileName: String): Nothing =
    throw DotEnvException("Unable to find env file under path '$fileName'!")
