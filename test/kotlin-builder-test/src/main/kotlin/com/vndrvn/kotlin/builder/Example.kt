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
    @Builder.Constructor
    constructor(
        name: String,
        age: Int = 0,
        generic: T,
        nullable: Boolean?,
        foo: Foo,
        list: List<String>,
        set: Set<String>,
        map: Map<String, String>,
        mutableList: MutableList<String>,
        mutableSet: MutableSet<String>,
        mutableMap: MutableMap<String, String>,
        extraArg: String
    ) : this(
        name,
        age,
        generic,
        nullable,
        foo,
        list + extraArg,
        set,
        map,
        mutableList,
        mutableSet,
        mutableMap
    )

    @Builder
    data class Foo(
        val bar: String
    )
}
