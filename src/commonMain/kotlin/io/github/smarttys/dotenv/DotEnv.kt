package io.github.smarttys.dotenv

import io.github.smarttys.dotenv.internal.DotEnvParser
import io.github.smarttys.dotenv.internal.DotEnvReader
import io.github.smarttys.dotenv.internal.loadEnvironmentToSystem
import io.github.smarttys.dotenv.internal.marshallAndWriteEnvToFile
import kotlin.reflect.KProperty

/**
 *
 */
internal typealias EnvMap = Map<String, String>

/**
 * `DotEnv` instance can be configured in its `DotEnv {}` factory function using [DotEnvBuilder].
 *  For demonstration purposes or trivial usages, DotEnv [companion][DotEnv.Companion] can be used instead.
 */
sealed class DotEnv {
    internal abstract val envMap: EnvMap

    /**
     * Returns the value corresponding to the given key, or null
     * if no such key is present.
     */
    abstract operator fun get(key: String): String?

    /**
     * Returns the value for given [key] and transforms it with given [transform]
     * function or null if no such key is present.
     */
    operator fun <T> get(key: String, transform: (String) -> T): T? {
        val value = get(key) ?: return null
        return transform(value)
    }

    /**
     * Returns true if the map contains the specified [key].
     */
    operator fun contains(key: String): Boolean = envMap.containsKey(key)

    /**
     * Returns a new [Iterator] for all entries in this [DotEnv].
     */
    operator fun iterator() = envMap.iterator()

    /**
     * Returns the value for given [key], or throws an [NoSuchElementException] if no
     * such key is present.
     */
    fun getOrThrow(key: String) = get(key) ?: throwNoSuchElementException(key)

    /**
     * Returns the value for the given [key] if the value is present. Otherwise, returns
     * the result of the [defaultValue] function.
     */
    inline fun getOrElse(key: String, defaultValue: () -> String) = get(key) ?: defaultValue.invoke()

    /**
     * Load all entries from this [DotEnv] into the system environment.
     *
     * NOTE: "System environment" refers to different objects depending on the
     * target platform (System-Properties for JVM, Environment for Native).
     */
    fun loadIntoSystemEnvironment(overwrite: Boolean = true) = loadEnvironmentToSystem(envMap, overwrite)

    companion object {
        private const val DEFAULT_ENV_FILE_NAME = ".env"
        internal val DEFAULT_FILE_LIST get() = listOf(DEFAULT_ENV_FILE_NAME)

        val DEFAULT: DotEnv by lazy(LazyThreadSafetyMode.NONE) {
            val reader = DotEnvReader(DEFAULT_FILE_LIST, false)
            val parser = DotEnvParser(
                lenientKeyParsing = false,
                ignoreMalformedKeys = false,
                ignoreDuplicateKeys = false,
                decodeBlankValues = true,
                ignoreMalformedSubstitution = false,
                includeSystemEnvironment = false,
                systemEnvironmentMap = emptyMap()
            )

            DotEnvImpl(parser.parse(reader.readInputBytes()))
        }
    }
}

internal class DotEnvImpl(override val envMap: EnvMap) : DotEnv() {
    override fun get(key: String): String? = envMap[key]
}

/**
 * Returns the value of the property for the given object from this [DotEnv].
 * This implementation transforms the properties name from camel to upper
 * snake case and uses it as key.
 *
 * @param thisRef the object for which the value is requested (not used).
 * @param prop the metadata for the property, used to get the name of property
 * and lookup the value corresponding to this name in the env.
 *
 * @return the property value.
 * @Throws NoSuchElementException when the env doesn't contain value for the property name.
 */
operator fun DotEnv.getValue(thisRef: Any?, prop: KProperty<*>): String {
    val key = prop.name.camelToUpperSnakeCase()
    return this.getOrThrow(key)
}

/**
 * Add all environment variables from [other] [DotEnv] to [this]
 * [DotEnv] instance.
 *
 * @receiver the [DotEnv] to add values to
 * @param [other] the [DotEnv] instance to combine with this one
 * @return A new [DotEnv] instance combining the current instance and the provided one
 */
operator fun DotEnv.plus(other: DotEnv): DotEnv = DotEnvImpl(this.envMap + other.envMap)

/**
 * Removes all environment variables from [other] [DotEnv] to [this]
 * [DotEnv] instance.
 *
 * @receiver the [DotEnv] to remove values from
 * @param [other] the [DotEnv] instance to remove values from this one
 * @return A new [DotEnv] instance with all elements from [other] contained in [this] instance removed
 */
operator fun DotEnv.minus(other: DotEnv): DotEnv = DotEnvImpl(this.envMap.minus(other.envMap.keys))

/**
 * Writes all entries in [this] [DotEnv] instance into a newly created file
 * under given [filePath].
 *
 * @receiver the [DotEnv] to write environment variables from
 * @param [filePath] for the created file
 */
fun DotEnv.write(filePath: String) = marshallAndWriteEnvToFile(envMap, filePath)

private fun String.camelToUpperSnakeCase() = fold(StringBuilder(length shl 1)) { acc, char ->
    if (char.isUpperCase()) {
        if (acc.isNotEmpty()) acc.append('_')
        acc.append(char)
    } else acc.append(char.uppercaseChar())
}.toString()

private fun throwNoSuchElementException(key: String): Nothing =
    throw NoSuchElementException("Key $key is missing in this DotEnv instance!")
