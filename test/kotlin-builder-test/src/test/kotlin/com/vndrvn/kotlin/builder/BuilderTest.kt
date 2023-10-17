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

            list += "abc"
            set += "123"
            map += "abc" to "123"

            mutableList.add("abc")
            mutableSet.add("123")
            mutableMap["abc"] = "123"

            extraArg = "secondary constructor arg"
        }

        assertEquals("bob", example.name)
        assertEquals(5, example.age)
        assertEquals("something", example.generic)
        assertNull(example.nullable)
        assertEquals("baz", example.foo.bar)
        assertEquals(2, example.list.size)
        assertEquals("abc", example.list.firstOrNull())
        assertEquals("secondary constructor arg", example.list.lastOrNull())
        assertEquals("123", example.set.singleOrNull())
        assertEquals("abc", example.map.keys.singleOrNull())
        assertEquals("123", example.map.values.singleOrNull())
        assertEquals("abc", example.mutableList.singleOrNull())
        assertEquals("123", example.mutableSet.singleOrNull())
        assertEquals("abc", example.mutableMap.keys.singleOrNull())
        assertEquals("123", example.mutableMap.values.singleOrNull())
    }
}
