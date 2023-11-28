package tech.relaycorp.courier.data.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class MessageTypeTest {
    @Test
    internal fun type() {
        assertEquals(
            MessageType.Cargo,
            MessageType.fromValue(MessageType.Cargo.value),
        )
        assertEquals(
            MessageType.CCA,
            MessageType.fromValue(MessageType.CCA.value),
        )
        assertThrows<IllegalArgumentException> {
            MessageType.fromValue("invalid")
        }
    }
}
