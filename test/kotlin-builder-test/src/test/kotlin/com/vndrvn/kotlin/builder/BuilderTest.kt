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

            // TODO
            // - Generate builder for nested classes to enable `foo = Foo { bar = "" }`
            // - Generate nested builder for nested classes to enable `foo { bar = "" }`
            foo = Foo {
                bar = "baz"
            }
        }

        assertEquals("bob", example.name)
        assertEquals(5, example.age)
        assertEquals("something", example.generic)
        assertNull(example.nullable)
        assertEquals("baz", example.foo.bar)
    }
}
