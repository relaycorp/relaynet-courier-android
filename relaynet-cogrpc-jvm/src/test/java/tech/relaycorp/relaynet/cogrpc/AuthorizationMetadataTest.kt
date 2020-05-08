package tech.relaycorp.relaynet.cogrpc

import io.grpc.Metadata
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class AuthorizationMetadataTest {

    @Test
    fun makeMetadata() {
        val cca = "abc"
        val metadata = AuthorizationMetadata.makeMetadata(cca.toByteArray())

        val auth = metadata[AuthorizationMetadata.MetadataKey]!!
        assertTrue(auth.startsWith(AuthorizationMetadata.AUTHORIZATION_TYPE))
    }

    @Test
    internal fun `getCCASerialized with valid authorization`() {
        val cca = "abc".toByteArray()
        val metadata = AuthorizationMetadata.makeMetadata(cca)

        assertEquals(
            cca.toList(),
            AuthorizationMetadata.getCCASerialized(metadata)!!.toList()
        )
    }

    @Test
    internal fun `getCCASerialized with invalid authorization`() {
        val metadata = Metadata().also { it.put(AuthorizationMetadata.MetadataKey, "INVALID") }

        assertNull(AuthorizationMetadata.getCCASerialized(metadata))
    }

    @Test
    internal fun `getCCASerialized with empty authorization`() {
        val metadata = Metadata()

        assertNull(AuthorizationMetadata.getCCASerialized(metadata))
    }
}
