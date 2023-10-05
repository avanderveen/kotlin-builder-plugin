package com.vndrvn.kotlin.builder

fun <T, U> T?.pairWith(other: U) = this?.let { Pair(it, other) }
