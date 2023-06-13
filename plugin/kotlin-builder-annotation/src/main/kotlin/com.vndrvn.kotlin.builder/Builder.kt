package com.vndrvn.kotlin.builder

@Target(AnnotationTarget.CLASS)
annotation class Builder(
    val name: String = ""
)
