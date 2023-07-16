package io.github.smarttys.dotenv.internal

import io.github.smarttys.dotenv.DotEnv
import io.github.smarttys.dotenv.EnvMap
import io.github.smarttys.dotenv.exception.MissingEnvFileException

internal class DotEnvReader(private val files: List<String>, private val ignoreMissingFile: Boolean) {

    fun readInputBytes(): ByteArray {
        return if (files.size > 1) {
            files.fold(byteArrayOf()) { acc, file ->
                val fileBytes = readInputFile(file)
                acc + fileBytes
            }
        } else readInputFile(files.single())
    }

    private fun readInputFile(file: String): ByteArray {
        return readFile(file) ?: if (ignoreMissingFile) {
            byteArrayOf()
        } else throwMissingFileException(file)
    }

    companion object {
        operator fun invoke(
            directory: String,
            files: Collection<String>,
            ignoreMissingFile: Boolean,
            systemEnvMap: EnvMap
        ): DotEnvReader {
            val filenamesOrDefault = files.ifEmpty(DotEnv::DEFAULT_FILE_LIST).map {
                val parsedFileName = it.removePrefix("/")
                val expandedPath = expandVariables("FilePath", directory, false, systemEnvMap)
                val expandedFileName = expandVariables("FileName", parsedFileName, false, systemEnvMap)

                expandedPath + expandedFileName
            }

            return DotEnvReader(filenamesOrDefault, ignoreMissingFile)
        }
    }
}

internal expect fun readFile(filePath: String): ByteArray?
internal expect fun readEnvironmentMap(): EnvMap

private fun throwMissingFileException(fileName: String): Nothing =
    throw MissingEnvFileException("Could not found file $fileName!")
