package io.github.smarttys.dotenv.internal

import io.github.smarttys.dotenv.EnvMap
import io.github.smarttys.dotenv.exception.DotEnvException
import io.github.smarttys.dotenv.exception.INVALID_KEY_FORMAT
import io.github.smarttys.dotenv.exception.KeyFormatErrorCode
import io.github.smarttys.dotenv.exception.MISSING_KEY_SEPARATOR_SIGN

internal class DotEnvParser(
    private val lenientKeyParsing: Boolean,
    private val ignoreDuplicateKeys: Boolean,

    private val decodeBlankValues: Boolean,
    private val ignoreMalformedSubstitution: Boolean,

    private val includeSystemEnvironment: Boolean,
    private val systemEnvironmentMap: EnvMap
) {

    /**
     * Current starting position index
     */
    private var startingIndex = 0
    private val exportByteArray by lazy(LazyThreadSafetyMode.NONE) { EXPORT_PREFIX_BYTES }

    /**
     * Initialise [LineCursor] to track line positions.
     */
    private var lineCursor = LineCursor()

    private fun reset() {
        startingIndex = 0
        lineCursor = LineCursor()
    }

    fun parse(files: List<String>, ignoreMissingFile: Boolean): EnvMap {
        val combinedEnv = files.fold(emptyMap<String, String>()) { map, file ->
            val fileBytes = readInputFile(file, ignoreMissingFile) ?: return@fold map
            val parsed = parse(fileBytes).also {
                reset()
            }

            map + parsed
        }

        return combinedEnv
    }

    fun parse(input: ByteArray): EnvMap {
        val envMap = if (includeSystemEnvironment) {
            systemEnvironmentMap.toMutableMap()
        } else mutableMapOf()

        while (startingIndex <= input.lastIndex) {
            updateStatementStartingIndex(input) ?: break

            val key = locateKeyName(input)
            val value = extractAndParseValue(key, input, envMap)

            if (key == null || this.decodeBlankValues.not() && value.isBlank()) continue
            if (!this.ignoreDuplicateKeys && envMap.containsKey(key)) {
                throw DotEnvException("Found duplicate key in env file: $key")
            }

            envMap[key] = value
        }

        return envMap
    }

    /**
     * Find the next statement start index in given [byteArray] and set [startingIndex]
     * to the found index.
     *
     * @return - [Unit] or null if no new starting position is found
     */
    private tailrec fun updateStatementStartingIndex(byteArray: ByteArray): Unit? {
        /**
         * Find the first non-whitespace character in the byte array starting at
         * the current [startingIndex] and update [startingIndex] to it's position in the
         * byte array.
         *
         * If no character can be found we return null as there are no key-value
         * pairs left to process anymore.
         */
        var found = false
        for (index in startingIndex..byteArray.lastIndex) {
            val byte = byteArray[index]
            if (byte.isLineSeparator) lineCursor++

            val char = byte.toInt().toChar()
            if (!char.isWhitespace() && byte != 0x0.toByte()) {
                startingIndex = index
                found = true

                /**
                 * If the found character is not the comment marker return
                 */
                if (byte.isCommentMarker.not()) return Unit
                break
            }
        }

        /**
         * No non-whitespace character found, so we return null to break
         * the for-loop of the calling function.
         */
        if (!found) return null

        /**
         * If we did not return in the previous statement we found a
         * comment marker and need to skip the comment now.
         */
        found = false
        for (index in startingIndex..byteArray.lastIndex) {
            /**
             * Skip all chars until we reached a line separator which indicates
             * the end of the comment
             * todo: we could also just directly return now dont we?
             */
            if (byteArray[index].isLineSeparator) {
                startingIndex = index
                // return updateStatementStartingIndex(byteArray)
                found = true
                break
            }
        }

        // return null
        if (!found) return null
        return updateStatementStartingIndex(byteArray)
    }

    private fun locateKeyName(input: ByteArray): String? {
        /**
         * Update starting index to find the first key char
         */
        updateToKeyPrefixEnd(input)

        var key: String? = null
        var offset = input.size
        var malformed = false
        var errorCode: KeyFormatErrorCode = MISSING_KEY_SEPARATOR_SIGN

        for (index in startingIndex..input.lastIndex) {
            when (val byte = input[index]) {
                DEFAULT_KEY_SEPARATION_MARKER, YAML_STYLE_KEY_SEPARATION_MARKER -> {
                    /**
                     * Skip the key separation marker
                     */
                    offset = index + 1

                    /**
                     * Key is marked as malformed (using not allowed characters) or
                     * is a blank string (length equals 0).
                     */
                    if (malformed || index == startingIndex) {
                        errorCode = INVALID_KEY_FORMAT
                        break
                    }

                    key = input.decodeToString(startingIndex, index)
                    break
                }
                '_'.code.toByte() -> {
                    /**
                     * The first character should be a letter
                     */
                    if (index == startingIndex) {
                        malformed = lenientKeyParsing.not()
                    }
                    continue
                }
                else -> {
                    val byteChar = byte.toInt().toChar()
                    if (byteChar.isLetterOrDigit()) {
                        /**
                         * The first character should be a letter
                         */
                        if (index == startingIndex && byteChar.isDigit()) {
                            malformed = lenientKeyParsing.not()
                        }
                        continue
                    }

                    /**
                     * We set the malformed flag to the opposite of the [lenientKeyParsing]
                     * settings. This leads to the following scenarios:
                     *
                     * - If an unauthorized character is set and lenientKeyParsing is set
                     *   to false, malformed is set to true.
                     * - In other cases malformed is always set to false.
                     */
                    malformed = lenientKeyParsing.not()

                    if (byte.isLineSeparator) {
                        offset = index
                        break
                    }
                }
            }
        }

        if (key == null) {
            when (errorCode) {
                INVALID_KEY_FORMAT -> throwInvalidKeyFormatException(input, offset - 1) { malformedKey ->
                    "Malformed or blank environment variable key near '$malformedKey' @ line ${lineCursor.position}"
                }
                MISSING_KEY_SEPARATOR_SIGN -> throwInvalidKeyFormatException(input, offset) { malformedKey ->
                    "Missing key separation sign ('=' / ':') for key '$malformedKey' @ line ${lineCursor.position}"
                }
            }
            return null
        }

        /**
         * Trim leading whitespaces but stop at line separators
         * in order to properly support unquoted empty values
         */
        for (toIndexUpdate in offset..input.size) {
            if (input[toIndexUpdate].isSpace.not()) {
                startingIndex = toIndexUpdate
                break
            }
        }

        return key
    }

    /**
     * Skips all leading whitespaces, 'export' prefix
     * and trailing whitespaces from the current key
     * name
     */
    private fun updateToKeyPrefixEnd(input: ByteArray) {
        /**
         * Checks whether [this] [ByteArray] starts with the export prefix
         * starting at given [startingIndex].
         */
        fun ByteArray.hasExportPrefix(startingIndex: Int): Boolean {
            for ((prefixIndex, index) in (startingIndex until startingIndex + EXPORT_PREFIX_LENGTH).withIndex()) {
                if (this[index] != exportByteArray[prefixIndex]) return false
            }

            return true
        }

        /**
         * Trim leading spaces. Optimal if no whitespace is expected
         * as the setter won't get called at all for keys without
         * a leading space.
         */
        for (index in startingIndex..input.lastIndex) {
            if (input[startingIndex].isWhitespace().not()) break
            startingIndex = index
        }

        /**
         * Trim possible "export" prefix if it's followed by at least one whitespace.
         * We expect at least one space in case of correct usage, so we continue until
         * we find a whitespace and then update [startingIndex].
         */
        if (input.hasExportPrefix(startingIndex)) {
            var trailingSpaces = 0
            for (index in startingIndex + EXPORT_PREFIX_LENGTH..input.lastIndex) {
                /**
                 * Trim trailing spaces but do not count line breaks as space
                 */
                if (input[index].isSpace) continue
                trailingSpaces++
            }

            /**
             * Expect at least one whitespace after the 'export' prefix otherwise
             * do not update [startingIndex],
             */
            if (trailingSpaces > 0) startingIndex += (EXPORT_PREFIX_LENGTH + trailingSpaces)
        }
    }

    private fun extractAndParseValue(key: String?, src: ByteArray, envMap: EnvMap): String {
        val quotation = src.locateQuote(startingIndex)

        fun skipOrDecode(startIndex: Int, endIndex: Int, decoder: (startIndex: Int, endIndex: Int) -> String): String {
            /**
             * Indicates to skip decoding for this value as the key is malformed
             */
            if (key == null) return ""

            /**
             * If the capacity equals 0 or 1 we can return with
             * a fast route, otherwise use the provided [decoder].
             */
            return when (endIndex - startIndex) {
                0 -> ""
                1 -> src[startIndex].toInt().toChar().toString()
                else -> {
                    val decoded = decoder(startIndex, endIndex)

                    /**
                     * Single-quoted (') values are used literally
                     * Unquoted and double-quoted (") values have parameter expansion applied.
                     */
                    if (quotation.isSingleQuote) {
                        decoded
                    } else {
                        expandVariables(key, decoded, ignoreMalformedSubstitution, envMap)
                    }
                }
            }
        }

        if (quotation == null) {
            var end = src.size
            for (index in startingIndex..src.lastIndex) {
                val indexByte = src[index]

                /**
                 * If the value is unquoted split at the next line separator or the
                 * comment marker (#) with a preceding whitespace
                 *
                 * If we found a valid comment, set the ending string index to the
                 * current index - 1 to remove the preceding whitespace from the value.
                 */
                val isLineSeparator = indexByte.isLineSeparator
                if (isLineSeparator || indexByte.isCommentMarker && src[index - 1].isWhitespace()) {
                    end = if (isLineSeparator) index else index - 1
                    break
                }
            }

            return skipOrDecode(startingIndex, end) { startIndex, endIndex ->
                src.decodeToString(startIndex, endIndex)
            }.also { startingIndex = end }
        }

        /**
         * Set the start index to the current position + 1 for the
         * quotation character ( " / ' )
         */
        var i = ++startingIndex
        do {
            val char = src[i]
            if (char != quotation) {
                when {
                    char.isLineSeparator -> {
                        lineCursor++
                    }
                    /**
                     * If we find an escape character we skip the next character
                     */
                    char.isEscape && i < src.lastIndex -> {
                        i++
                    }
                }
                continue
            }

            /**
             * Extract value between the opening and closing quotation characters
             */
            val value = skipOrDecode(startingIndex, i) { startIndex, endIndex ->
                decodeQuotedValue(src, startIndex, endIndex, quotation)
            }

            startingIndex = i + 1
            return value
        } while (i++ < src.lastIndex)

        throw DotEnvException("Quoted value is unterminated for key $key @ ${lineCursor.position}")
    }

    private fun decodeQuotedValue(src: ByteArray, startIndex: Int, endIndex: Int, quotation: Byte): String {
        return if (quotation.isDoubleQuote) {
            decodeAndExpandEscapes(src, startIndex, endIndex) { charByte ->
                /**
                 * Common shell escape sequences including \n, \r, \t, and \\ are
                 * supported in double-quoted values.
                 */
                when (charByte) {
                    'b'.code.toByte() -> 0x8.toByte() // 8
                    't'.code.toByte() -> 0x9.toByte() // 9
                    'n'.code.toByte() -> 0xA.toByte() // 10
                    'r'.code.toByte() -> 0xD.toByte() // 13
                    else -> charByte
                }
            }
        } else decodeAndExpandEscapes(src, startIndex, endIndex) { charByte ->
            /**
             * Unescape single quotes
             */
            if (charByte.isSingleQuote) charByte else null
        }
    }

    /**
     * Escapes are expanded for double quoted values
     */
    private fun decodeAndExpandEscapes(input: ByteArray, startIndex: Int, endIndex: Int, transformer: (Byte) -> Byte?): String {
        var stringIndex = startIndex

        var byteArrayPosition = 0
        val byteArray = ByteArray(endIndex - startIndex)

        while (stringIndex < endIndex) {
            val sbByte = input[stringIndex]
            if (sbByte.isEscape) {
                val appendedChar = input[stringIndex + 1]
                val newChar = transformer(appendedChar)

                if (appendedChar.isExpansionMarker.not() && newChar != null) {
                    byteArray[byteArrayPosition++] = newChar
                    stringIndex += 2

                    continue
                }
            }

            stringIndex++
            byteArray[byteArrayPosition++] = sbByte
        }

        return byteArray.decodeToString(0, byteArrayPosition)
    }

    private inline fun throwInvalidKeyFormatException(
        input: ByteArray,
        keyEndIndex: Int,
        crossinline messageBuilder: (key: String) -> String
    ) {
        val malformedKey = input.decodeToString(startingIndex, keyEndIndex)
        throw DotEnvException(messageBuilder(malformedKey))
    }
}
