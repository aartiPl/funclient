package net.igsoft.typeutils.globalcontext

import assertk.assertThat
import assertk.assertions.isEqualTo
import net.igsoft.typeutils.marker.TypedMarker
import org.junit.jupiter.api.Test

data class Person(val firstName: String, val lastName: String, val age: Int)

class GlobalContextTest {
    private val person by TypedMarker.create<Person>()

    @Test
    fun `Assert that we can save and read objects on GlobalContext`() {
        val personEntity = Person("Marcin", "Iksiński", 28)
        GlobalContext.register(person, personEntity)

        assertThat(GlobalContext[person]).isEqualTo(personEntity)
    }
}
