package com.vndrvn.kotlin.builder

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BuilderTest {
    @Test
    fun blah() {
        val example = Example<String> {
            name = "bob"
            age = 5
            generic = "something"
            nullable = null
        }

        assertEquals("bob", example.name)
        assertEquals(5, example.age)
        assertEquals("something", example.generic)
        assertNull(example.nullable)
    }
}
