package io.github.smarttys.dotenv.internal

/**
 * "export" as byte array
 */
internal const val EXPORT_PREFIX_LENGTH = 6
internal inline val EXPORT_PREFIX_BYTES get() = byteArrayOf(0x65, 0x78, 0x70, 0x6F, 0x72, 0x74)

private const val COMMENT_MARKER = '#'.code.toByte()
private const val ESCAPE_MARKER = '\\'.code.toByte()
private const val EXPANSION_MARKER = '$'.code.toByte()

private const val SINGLE_QUOTE_PREFIX = '\''.code.toByte()
private const val DOUBLE_QUOTE_PREFIX = '"'.code.toByte()

internal const val DEFAULT_KEY_SEPARATION_MARKER = '='.code.toByte()
internal const val YAML_STYLE_KEY_SEPARATION_MARKER = ':'.code.toByte()

internal fun Byte.isWhitespace() = this.toInt().toChar().isWhitespace()

/**
 * Whether this [Byte] is a line end / line separator
 */
internal val Byte.isLineSeparator inline get() = this == '\n'.code.toByte() || this == '\r'.code.toByte()

/**
 * Whether this [Byte] is a whitespace, with the difference that
 * line breaks are not considered as space.
 *
 * '\v'.code.toByte(), '\f'.code.toByte(),
 */
internal val Byte.isSpace get() = when (this) {
    '\t'.code.toByte(), '\r'.code.toByte(), ' '.code.toByte() -> true
    0x85.toByte(), 0xA0.toByte() -> true
    else -> false
}

/**
 * Whether this [Byte] is an escape character
 */
internal inline val Byte.isEscape get() = this == ESCAPE_MARKER

/**
 * Whether this [Byte] marks the start of a comment block
 */
internal inline val Byte.isCommentMarker get() = this == COMMENT_MARKER

/**
 * Weather this [Byte] marks the start of a variable expansion block
 */
internal inline val Byte.isExpansionMarker get() = this == EXPANSION_MARKER

internal inline val Byte.isDoubleQuote inline get() = this == DOUBLE_QUOTE_PREFIX
internal inline val Byte?.isSingleQuote inline get() = this == SINGLE_QUOTE_PREFIX

internal fun ByteArray.locateQuote(index: Int): Byte? {
    return when (val byte = this[index]) {
        DOUBLE_QUOTE_PREFIX, SINGLE_QUOTE_PREFIX -> byte
        else -> null
    }
}
