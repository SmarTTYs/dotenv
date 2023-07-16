package io.github.smarttys.dotenv

import io.github.smarttys.dotenv.exception.DotEnvException
import io.github.smarttys.dotenv.exception.InvalidSubstitutionException
import io.github.smarttys.dotenv.internal.DotEnvParser
import io.github.smarttys.dotenv.internal.DotEnvReader
import io.github.smarttys.dotenv.internal.loadEnvironmentToSystem
import io.github.smarttys.dotenv.internal.readEnvironmentMap

/**
 * Creates an instance of [DotEnv] configured by the given [builderAction].
 *
 * Example usage:
 * ```
 * val dotEnv = DotEnv {
 *     file("your-env-file.env")
 *
 *     // configure DotEnv instance
 *     ignoreMissingFile = false
 *     ignoreMalformedKey = false
 *
 *     ignoreDuplicateKeys = true
 * }
 * ```
 */
inline fun DotEnv(builderAction: DotEnvBuilder.() -> Unit): DotEnv {
    val dotEnvBuilder = DotEnvBuilder().apply(builderAction)
    return dotEnvBuilder.build()
}

/**
 * Loads all environment variables found for the configuration based on
 * the given [builderAction] into the "system environment".
 *
 * Note that "system environment" has different meanings based on
 * the used platform.
 *
 */
@Suppress("FunctionName")
inline fun LoadEnv(overwrite: Boolean = true, builderAction: DotEnvBuilder.() -> Unit) {
    val dotEnvBuilder = DotEnvBuilder().apply(builderAction)
    dotEnvBuilder.load(overwrite)
}

/**
 * Creates an instance of [DotEnv] for the provided [filePath] without
 * any changes to the default configuration.
 *
 * @param [filePath] for the .env file
 * @return [DotEnv] instance with the configured [filePath].
 */
inline fun DotEnv(filePath: String): DotEnv = DotEnv {
    file(filePath)
}

/**
 * Loads all environment variables found in the file located under
 * given [filePath] into the "system environment".
 *
 * Note that "system environment" has different meanings based on
 * the used platform.
 * */
@Suppress("FunctionName")
inline fun LoadEnv(filePath: String, overwrite: Boolean = true) = LoadEnv(overwrite) {
    file(filePath)
}

/**
 * Builder of the [DotEnv] instance provided by `DotEnv { ... }` factory function.
 */
class DotEnvBuilder @PublishedApi internal constructor() {
    /**
     * List of filenames this dotenv implementation should
     * read from
     */
    private val files = mutableSetOf<String>()

    /**
     * Specifies whether missing files should be ignored and do not throw an exception on
     * parsing.
     *
     * When this flag is disabled missing files will not cause the reader to throw
     * an exception
     *
     * `false` by default
     */
    var ignoreMissingFile = false

    /**
     * Specifies whether encounters of malformed kes in the input .env file should
     * be ignored instead of throwing [DotEnvException].
     *
     * `false` by default.
     */
    var ignoreMalformedKeys = false

    /**
     * Specifies whether key parsing should be lenient.
     *
     * Lenient key parsing allows all characters outside the normally permitted
     * character set except for line breaks to be used in variable keys.
     *
     * 'false' by default
     */
    var lenientKeyParsing = false

    /**
     * Specifies whether encounters of duplicated keys in the input .env file should
     * be ignored instead of throwing [DotEnvException].
     *
     * When this flag is enabled a duplicate key overwrites the current value on the
     * parsing process.
     *
     * `false` by default.
     */
    var ignoreDuplicateKeys = false

    /**
     * Specifies whether encounters of malformed substitutions should be ignored.
     *
     * When this flag is disabled a malformed substitution will
     * throw a [InvalidSubstitutionException].
     *
     * `false` by default.
     */
    var ignoreMalformedSubstitution = false

    /**
     * Specifies whether to ignore blank values and do not include them into
     * the [DotEnv].
     *
     * When this flag is disabled all blank values will be ignored while parsing
     * the input.
     *
     * `true` by default
     */
    var decodeBlankValues = true

    /**
     * Specifies whether the created [DotEnv] entries should get load into
     * the system environment. This flag WILL NOT overwrite already existing
     * values.
     *
     * In order to fully overwrite the system environment use this flag in
     * combination with [overwriteExistingSystemVariables].
     *
     * NOTE: "system environment" has different meanings based on the platform:
     *  - JVM: The jvm system properties
     *  - Native: Current process / system environment
     *  - JS: Current process environment
     *
     *  `false` by default
     */
    var overloadSystemEnvironment = false

    /**
     * Specifies whether to overwrite already existing system variables when
     * overloading the current system environment.
     *
     * IMPORTANT: This flag only works if [overloadSystemEnvironment] is
     * enabled!
     *
     * 'true' by default
     */
    var overwriteExistingSystemVariables = true

    /**
     * Specifies whether the current system environment should be included
     * as the base for created [DotEnv] instance.
     *
     * This allows to use values use all system environment variables for
     * value substitution.
     *
     * In order to be able to overwrite environment variables define in
     * the process environment u need to enable the [ignoreDuplicateKeys]
     * flag
     *
     * 'false' by default
     */
    var includeSystemEnvironment = false

    /**
     * Specifies the base directory to search .env files in.
     *
     * `./` by default.
     */
    var directory = "./"
        set(value) {
            field = value.removeSuffix("/") + "/"
        }

    fun file(fileName: String) = apply { files.add(fileName) }
    fun files(vararg fileNames: String) = apply { files.addAll(fileNames) }

    @PublishedApi internal fun build(): DotEnv {
        val envMap = parseInput()

        if (this.overloadSystemEnvironment) {
            loadEnvironmentToSystem(envMap, overwriteExistingSystemVariables)
        }

        return DotEnvImpl(envMap)
    }

    @PublishedApi internal fun load(overwrite: Boolean) {
        val envMap = parseInput()
        loadEnvironmentToSystem(envMap, overwrite)
    }

    private fun parseInput(): EnvMap {
        val systemEnvMap = readEnvironmentMap()
        val reader = DotEnvReader(directory, files, ignoreMissingFile, systemEnvMap)

        val parser = DotEnvParser(
            lenientKeyParsing,
            ignoreMalformedKeys,
            ignoreDuplicateKeys,
            decodeBlankValues,
            ignoreMalformedSubstitution,
            includeSystemEnvironment,
            systemEnvMap
        )

        return parser.parse(reader.readInputBytes())
    }
}
