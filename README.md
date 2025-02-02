# DotEnv/KM

A Kotlin/Multiplatform library to load environment variables from a **.env** file
inspired by the Ruby [dotenv](https://github.com/bkeepers/dotenv) project
and the environment file handling in [docker-compose](https://docs.docker.com/compose/environment-variables/env-file/).
For more information check out our [api documentation](https://smarttys.github.io/dotenv/)

Why using .env files? From the original library:
> Storing configuration in the environment is one of the tenets of a
> [twelve-factor](https://12factor.net/config) app. Anything that is likely
> to change between deployment environments–such as resource handles for
> databases or credentials for external services–should be extracted from
> the code into environment variables.
>
> But it is not always practical to set environment variables on development
> machines or continuous integration servers where multiple projects are run. 
> Dotenv load variables from a .env file into ENV when the environment is
> bootstrapped.
>
> -- [Brandon Keepers](https://github.com/bkeepers/dotenv)

## Install
Install the dependency using Gradle or Maven.

### Gradle Groovy DSL
````groovy
implementation 'io.github.smarttys:dotenv:0.1'
````

### Gradle Kotlin DSL
````kotlin
implementation("io.github.smarttys:dotenv:0.1")
````

### Maven
````xml
<dependency>
    <groupId>io.github.smarttys</groupId>
    <artifactId>dotenv</artifactId>
    <version>0.1</version>
</dependency>
````

## Usage
Create a ``.env`` file in the root of your project:
````shell
S3_BUCKET="YOURS3BUCKET"
SECRET_KEY="YOURSECRETKEYGOESHERE"

PORT=8080
````

Then in your application create a new DotEnv instance...
````kotlin
// create new configured DotEnv instance
val dotEnv = DotEnv("file-name")

val s3Bucket = dotEnv["S3_BUCKET"]

// define default
val unknown = dotEnv.getOrElse("UNKNOWN") { "DEFAULT" }

// transform value
val port = dotEnv["PORT", { it.toInt() }] // transform input to int

// or using a reference
val port = dotEnv["PORT", String::toInt]

// loop through all key-value pairs
for ((key, value) in dotEnv) {
    println("Key: $key=$value")
}
````

or use the un-configured default instance:
````kotlin
/**
 * Default instance using '.env' as file name in the root directory './'
 */
val s3Bucket = DotEnv.DEFAULT["S3_BUCKET"]
````

## Delegated Property
Alternatively DotEnv supports kotlin's delegating property pattern to retrieve values from an
DotEnv instance.

In this case the key is the same as the name of the property
(*camelCase* is changed to *UPPER_SNAKE_CASE*)

````kotlin
// property name translates to SECRET_KEY
val secretKey by dotEnv
````


## Specify Key-Values
DotEnv supports two styles for defining values. The default one
````shell
KEY="VALUE"
````

and the YAML(ish) style
````yaml
KEY: "VALUE"
````

In addition, the export prefix can also be used:
````shell
export KEY="Value"
export HOST: 8080
````


## Parameter Expansion
Values in an .env file can be unquoted, single quoted, or double-quoted.
The value is handled depending on the quotation character:

### Single-Quotes
Single-quoted ``(')`` values are used literally.
``VAR='${OTHER}'`` -> ``${OTHER}``

### Double-Quotes / Unquoted
For double-quoted and unquoted values parameter expansion gets applied.
Both braced ``(${VAR})`` and unbraced ``($VAR)`` expressions are supported.

````shell
PORT=8080
HOST=localhost$PORT # equals: localhost:8080
````
For braced expressions, the following formats are supported:

### Direct substitution

``${VAR}`` -> value of ``VAR``

### Default value
``${VARIABLE:-default}`` -> value of `VARIABLE` if set and non-empty, otherwise `default`

``${VARIABLE-default`` -> value of `VARIABLE` if set, otherwise `default`

### Required value
``${VARIABLE:?default}`` -> value of `VARIABLE` if set and non-empty, otherwise throw exception with ``err`` message

``${VARIABLE:default}`` -> value of `VARIABLE` if set, otherwise throw exception with ``err`` message

### Alternative value / consider adding? - In Progress

### Substring
``${VARIABLE:1}`` -> substring of ``VARIABLE`` starting at `1`

``${VARIABLE:1:4}`` -> substring of ``VARIABLE`` from `1` to ``4``


## Multiline values
If you need multiline variables, for example private keys, u can use them in double-quotes
with line breaks:

````shell
PRIVATE_KEY="-----BEGIN RSA PRIVATE KEY-----
...
Kh9NV...
...
-----END RSA PRIVATE KEY-----"
````

Alternatively, you can use double-quoted strings and the ``\n`` character.

````shell
PRIVATE_KEY="-----BEGIN RSA PRIVATE KEY-----\Kh9NV...\n-----END RSA PRIVATE KEY-----"
````

## Comments
Comments may be added to your file on a separate line or as inline comment:
````shell
# This is a separate comment
PASSWORD=YOUR_PASSWOD # inline comment
HASH="double-quoted-value-containing-#-character"# inline comment
HOST='single-quoted-value-containing-#-character'# inline comment
````

Inline comments for unquoted values must be preceded with a space.
````shell
HOST=localhost#invalid-comment
PORT=8080 # valid comment
````

## Load into system-environment
If no DotEnv instance should be created, but the key-value pairs should only be loaded into the current environment, the LoadEnv function can be used.

The format and options are the same as when loading an ``.env`` file into a DotEnv instance.

**IMPORTANT**: Depending on the platform the definition of environment differs:
- JVM: Load into System-Properties
- Native: Load directly into process environment

````kotlin
/**
 * Load found environment variables to system environment and overwrite already
 * existing keys
 */
LoadEnv(".env")

// in order to not overwrite existing variables use:
LoadEnv(".env", overwrite = false)
````
All the [configurations options](#configuration-options) listed below can be used here as well.


## Write into file

A created DotEnv instance can be written directly to a file.
Values are written **expanded** and comments are **not preserved**.

````kotlin
val dotEnv = DotEnv("ignore_empty_option_test.env")
dotEnv.write("target-file-path.env")
````

This can also be useful to merge more than one file into a single one:

````kotlin
val dotEnv = DotEnv("first_file.env", "second_file.env")

// Will produce one single combined file
dotEnv.write("target-file-path.env")
````

## Configure
Configure ur DotEnv instance using the builder DSL.

````kotlin
val dotEnv = DotEnv {
    /**
     * Configure ur DotEnv instance
     */
}
````

### Configuration Options

The following options can be used to configure:

#### directory
Default: ``./``

Base directory path to search **.env** file(s) in.

````kotlin
val dotEnv = DotEnv {
    directory = "./assets"
}
````

#### ignoreMissingFile
Default: ``false``

Do not throw when **.env** file does not exist. Dotenv will continue to retrieve
environment variables from other files or the system environment.

````kotlin
val dotEnv = DotEnv {
    ignoreMissingFile = false
}
````

#### lenientKeyParsing
Default: ``false``

Lenient key parsing allows all characters outside the normally permitted
character set except for line breaks to be used in variable keys.

````kotlin
val dotEnv = DotEnv {
    lenientKeyParsing = true
}
````

#### ignoreDuplicateKeys
Default: ``false``

Do not throw an exception if a key appears more than once in the loaded 
.env files. Only the first key-value entry will be written to the DotEnv
instance.

````kotlin
val dotEnv = DotEnv {
    ignoreDuplicateKeys = false
}
````

#### ignoreMalformedSubstitution
Default: ``false``

Do not throw an exception for malformed parameter expansion.

````kotlin
val dotEnv = DotEnv {
    ignoreMalformedSubstitution = false
}
````

#### decodeBlankValues
Default: ``true``

Whether to include blank values into the created DotEnv instance.

````kotlin
val dotEnv = DotEnv {
    ignoreBlankValues = false
}
````

##### includeSystemEnvironment
Default: ``false``

Defines whether existing system environment variables should 
also be loaded into the DotEnv instance.

This way you get a combined DotEnv instance which also holds the
current system environment and the already existing system variables
can be expanded within the loaded file.

````kotlin
val dotEnv = DotEnv {
    includeSystemEnvironment = true
}
````
