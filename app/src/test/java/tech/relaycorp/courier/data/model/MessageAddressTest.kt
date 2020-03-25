package tech.relaycorp.courier.data.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class MessageAddressTest {

    @Test
    internal fun type() {
        assertEquals(
            MessageAddress.Type.Private,
            MessageAddress.Type.fromValue(MessageAddress.Type.Private.value)
        )
        assertEquals(
            MessageAddress.Type.Public,
            MessageAddress.Type.fromValue(MessageAddress.Type.Public.value)
        )
        assertThrows<IllegalArgumentException> {
            MessageAddress.Type.fromValue("invalid")
        }
    }
}
