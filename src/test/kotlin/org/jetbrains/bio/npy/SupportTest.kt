package org.jetbrains.bio.npy

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class TestParseDict(private val expected: Map<String, Any>,
                    private val s: String) {

    @Test fun parse() {
        assertEquals(expected, parseDict(s))
    }

    companion object {
        @JvmStatic
        @Parameters(name = "{1}")
        fun `data`() = listOf(
                arrayOf(emptyMap<String, String>(), "{}"),
                arrayOf(mapOf("foo" to "bar"), "{'foo': 'bar'}"),
                arrayOf(mapOf("foo" to false), "{'foo': False}"),
                arrayOf(mapOf("foo" to "False"), "{'foo': 'False'}"),
                arrayOf(mapOf("foo" to true), "{'foo': True}"),
                arrayOf(mapOf("foo" to emptyList<Int>()), "{'foo': ()}"),
                arrayOf(mapOf("foo" to listOf(1, 2)), "{'foo': (1, 2, )}"),
                arrayOf(mapOf("foo" to emptyList<Int>(),
                              "boo" to "bar"),
                        "{'foo': (), 'boo': 'bar'}"),
                arrayOf(mapOf("foo" to emptyList<Int>(),
                              "boo" to "bar"),
                        "{'foo'  :   ()  ,    'boo': 'bar'}"))
    }
}