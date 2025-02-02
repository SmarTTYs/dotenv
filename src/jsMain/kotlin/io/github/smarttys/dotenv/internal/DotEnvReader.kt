package io.github.smarttys.dotenv.internal

import io.github.smarttys.dotenv.EnvMap

/**
 * Reads the current process environment
 */
internal actual fun readEnvironmentMap(): EnvMap = process.env.asMap

internal val fs by lazy { js("require('fs')") }

/**
 * Wrapper for the Javascript `process` object.
 */
internal external val process: Process

internal external interface Process {

    /**
     * Wrapper for the Javascript `process.env` object.
     */
    val env: ProcessEnv
}

internal external interface ProcessEnv : Dict<String>

internal external interface Dict<T>

internal operator fun <T> Dict<T>.get(key: String): T? = this.asDynamic()[key] as? T
internal operator fun <T> Dict<T>.set(key: String, value: T?) { this.asDynamic()[key] = value }
internal fun <T> Dict<T>.contains(key: String) = this[key] != null

private interface Entry
private val Entry.key get() = this.asDynamic()[0] as String
private val Entry.value get() = this.asDynamic()[1] as String

private val ProcessEnv.entries get() = js("Object").entries(this).unsafeCast<Array<Entry>>()

private val ProcessEnv.asMap get(): Map<String, String> {
    val map = mutableMapOf<String, String>()
    for (entry in entries) {
        map[entry.key] = entry.value
    }

    return map
}

@Suppress("SwallowedException")
internal actual fun readFile(filePath: String): ByteArray? {
    val fileString = try {
        fs.readFileSync(filePath, "utf8") as String
    } catch (e: Throwable) {
        return null
    }

    return fileString.encodeToByteArray()
}
