package com.vndrvn.kotlin.builder

@Builder
data class Example<T : Any>(
    val name: String,
    val age: Int = 0,
    val generic: T,
    val nullable: Boolean?
)
