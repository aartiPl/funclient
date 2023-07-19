package net.igsoft.typeutils.marker

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import org.junit.jupiter.api.Test

class TypedMarkerTest {
    @Test
    fun `Assert that TypedMarker can be created with property syntax`() {
        val someProperty by TypedMarker.create<String>()
        assertThat(someProperty).apply {
            prop(TypedMarker<String>::clazz).isEqualTo(String::class.java)
            prop(TypedMarker<String>::id).isEqualTo("someProperty")
        }
    }

    @Test
    fun `Assert that TypedMarker can be created manually`() {
        assertThat(TypedMarker.create(String::class.java, "s1")).apply {
            prop(TypedMarker<String>::clazz).isEqualTo(String::class.java)
            prop(TypedMarker<String>::id).isEqualTo("s1")
        }

        assertThat(TypedMarker.create<Int>("s2")).apply {
            prop(TypedMarker<Int>::clazz).isEqualTo(Integer::class.java)
            prop(TypedMarker<Int>::id).isEqualTo("s2")
        }
    }
}
