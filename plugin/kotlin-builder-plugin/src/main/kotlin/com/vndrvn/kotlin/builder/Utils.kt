package com.vndrvn.kotlin.builder

fun <T, U> T?.pairWith(other: U) = this?.let { Pair(it, other) }

fun String.withCasing(casing: Casing) = when (casing) {
    Casing.PascalCase -> first().uppercase() + skip(1)
    Casing.CamelCase -> first().lowercase() + skip(1)
    else -> this
}

fun String.skip(n: Int) = if (length < n) "" else substring(n)
