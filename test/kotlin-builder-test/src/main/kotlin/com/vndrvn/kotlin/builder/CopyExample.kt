package com.vndrvn.kotlin.builder

@Builder
class CopyExample<T>(
    val test: T,
    val test2: Int
) {
    @Builder.Constructor
    constructor(
        test: T,
        test2: String
    ) : this(
        test,
        test2.toInt()
    )
}
