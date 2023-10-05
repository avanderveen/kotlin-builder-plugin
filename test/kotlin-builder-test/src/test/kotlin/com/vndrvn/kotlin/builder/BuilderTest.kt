package com.vndrvn.kotlin.builder

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BuilderTest {
    @Test
    fun `happy path`() {
        val example = Example<String> {
            name = "bob"
            age = 5
            generic = "something"
            nullable = null
            foo = Foo {
                bar = "baz"
            }

            // TODO add builders for collection types (maps, arrays, lists, sets, etc.)
            list = listOf("abc")
        }

        assertEquals("bob", example.name)
        assertEquals(5, example.age)
        assertEquals("something", example.generic)
        assertNull(example.nullable)
        assertEquals("baz", example.foo.bar)
    }
}
