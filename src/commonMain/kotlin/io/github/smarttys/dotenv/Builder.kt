package io.github.smarttys.dotenv

import io.github.smarttys.dotenv.exception.DotEnvException
import io.github.smarttys.dotenv.internal.*
import io.github.smarttys.dotenv.internal.DotEnvParser
import io.github.smarttys.dotenv.internal.readEnvironmentMap

@PublishedApi
internal const val DEFAULT_ENV_FILE_NAME: String = ".env"

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
public inline fun DotEnv(builderAction: DotEnvBuilder.() -> Unit): DotEnv {
    val dotEnvBuilder = DotEnvBuilder(DEFAULT_ENV_FILE_NAME).apply(builderAction)
    return dotEnvBuilder.build()
}

public inline fun DotEnv(filePath: String, builderAction: DotEnvBuilder.() -> Unit): DotEnv {
    val dotEnvBuilder = DotEnvBuilder(filePath).apply(builderAction)
    return dotEnvBuilder.build()
}

/**
 * Creates an instance of [DotEnv] for the provided [basePath] without
 * any changes to the default configuration.
 *
 * @param [basePath] for the .env file
 * @return [DotEnv] instance with the configured [basePath].
 */
@ExperimentalDotEnvApi
public inline fun DotEnv(
    basePath: String,
    vararg overwritingPaths: String,
    builderAction: DotEnvBuilder.() -> Unit = {}
): DotEnv {
    val initialEnv = DotEnv(basePath, builderAction)
    val combinedEnv = overwritingPaths.fold(initialEnv) { env, file ->
        val fileEnv = DotEnv(file, builderAction)
        env + fileEnv
    }

    return combinedEnv
}

/**
 * Loads all environment variables found for the configuration based on
 * the given [builderAction] into the "system environment".
 *
 * Note that "system environment" has different meanings based on
 * the used platform.
 *
 * For further information see [DotEnv.loadIntoSystemEnvironment]
 */
@Suppress("FunctionName")
public inline fun LoadEnv(filePath: String, overwrite: Boolean = true, builderAction: DotEnvBuilder.() -> Unit) {
    val dotEnv = DotEnv(filePath, builderAction)
    dotEnv.loadIntoSystemEnvironment(overwrite)
}

/**
 * Loads all environment variables found in the file located under
 * given [basePath] into the "system environment", overwriting with
 * all variables found in the [overwritingPaths].
 *
 * Note that "system environment" has different meanings based on
 * the used platform.
 * */
@OptIn(ExperimentalDotEnvApi::class)
@Suppress("FunctionName")
public fun LoadEnv(basePath: String, vararg overwritingPaths: String, overwrite: Boolean = true) {
    val dotEnv = DotEnv(basePath, *overwritingPaths)
    dotEnv.loadIntoSystemEnvironment(overwrite)
}

/**
 * Builder of the [DotEnv] instance provided by `DotEnv { ... }` factory function.
 */
public class DotEnvBuilder @PublishedApi internal constructor(private val filePath: String) {
    /**
     * Specifies whether missing files should be ignored and do not throw an exception on
     * parsing.
     *
     * When this flag is disabled missing files will not cause the reader to throw
     * an exception
     *
     * `false` by default
     */
    public var ignoreMissingFile: Boolean = false

    /**
     * Specifies whether key parsing should be lenient.
     *
     * Lenient key parsing allows all characters outside the normally permitted
     * character set except for line breaks to be used in variable keys as well
     * as digits and underscores at the start of the variable name.
     *
     * NOTE: empty keys will still throw an exception as multiple tools do not
     * properly handle empty keys and because it should be discouraged to use
     * them at all.
     *
     * 'false' by default
     */
    public var lenientKeyParsing: Boolean = false

    /**
     * Specifies whether encounters of duplicated keys in the input .env file should
     * be ignored instead of throwing [DotEnvException].
     *
     * When this flag is enabled a duplicate key overwrites the current value on the
     * parsing process.
     *
     * `false` by default.
     */
    public var ignoreDuplicateKeys: Boolean = false

    /**
     * Specifies whether encounters of malformed substitutions should be ignored.
     *
     * When this flag is disabled a malformed substitution will
     * throw a [DotEnvException].
     *
     * `false` by default.
     */
    public var ignoreMalformedSubstitution: Boolean = false

    /**
     * Specifies whether to ignore blank values and do not include them into
     * the [DotEnv].
     *
     * When this flag is disabled all blank values will be ignored while parsing
     * the input.
     *
     * `true` by default
     */
    public var decodeBlankValues: Boolean = true

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
    public var includeSystemEnvironment: Boolean = false

    /**
     * Specifies the base directory to search .env files in.
     *
     * `./` by default.
     */
    public var directory: String = "./"
        set(value) {
            field = value.removeSuffix("/") + "/"
        }

    @PublishedApi internal fun build(): DotEnv {
        val systemEnvMap = readEnvironmentMap()
        val parser = DotEnvParser(
            lenientKeyParsing,
            ignoreDuplicateKeys,
            decodeBlankValues,
            ignoreMalformedSubstitution,
            includeSystemEnvironment,
            systemEnvMap
        )

        val expandedFileName = getExpandedFilePath(directory, filePath, systemEnvMap)
        val envMap = parser.parse(expandedFileName, ignoreMissingFile)
        return DotEnvImpl(envMap)
    }
}
