package com.vndrvn.kotlin.builder

@Target(AnnotationTarget.CLASS)
annotation class Builder(
    val casing: Casing = Casing.Default,
    val name: String = ""
) {
    /**
     * Used to mark a secondary constructor as the Builder target (optional)
     */
    @Target(AnnotationTarget.CONSTRUCTOR)
    annotation class Constructor
}

enum class Casing {
    /**
     * Name the builder function after the target class, with no change to casing.
     *
     * Note: This is overridden if casing is configured at the project level in Gradle.
     */
    Default,

    /**
     * camelCase names start with a lowercase
     */
    CamelCase,

    /**
     * PascalCase names start with an uppercase
     */
    PascalCase
}
