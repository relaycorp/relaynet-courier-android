package tech.relaycorp.courier.data.model

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GatewayTypeTest {
    @Test
    internal fun fromValue() {
        Assertions.assertEquals(
            GatewayType.Private,
            GatewayType.fromValue(GatewayType.Private.value)
        )
        Assertions.assertEquals(
            GatewayType.Internet,
            GatewayType.fromValue(GatewayType.Internet.value)
        )
        assertThrows<IllegalArgumentException> {
            GatewayType.fromValue("invalid")
        }
    }
}
