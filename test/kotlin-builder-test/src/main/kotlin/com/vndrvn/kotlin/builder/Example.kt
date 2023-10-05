package com.vndrvn.kotlin.builder

@Builder
data class Example<T : Any>(
    val name: String,
    val age: Int = 0,
    val generic: T,
    val nullable: Boolean?,
    val foo: Foo,
    val list: List<String>,
    val set: Set<String>,
    val map: Map<String, String>,
    val mutableList: MutableList<String>,
    val mutableSet: MutableSet<String>,
    val mutableMap: MutableMap<String, String>
) {
    @Builder
    data class Foo(
        val bar: String
    )
}
