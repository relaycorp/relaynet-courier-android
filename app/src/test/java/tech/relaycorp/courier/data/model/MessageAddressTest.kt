package tech.relaycorp.courier.data.model

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.relaycorp.doh.Answer
import tech.relaycorp.doh.DoHClient
import tech.relaycorp.doh.LookupFailureException

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

class PublicMessageAddressTest {
    @Nested
    inner class Resolve {
        private val dohClient = mock<DoHClient>()

        private val originalHost = "example.com"
        private val originalPublicAddress = "https://$originalHost"
        private val targetHost = "target.example.com"
        private val targetPort = "1357"
        private val srvRecordName = "_awala-crc._tcp.$originalHost"
        private val srvRecordData = "0 1 $targetPort $targetHost."

        private val publicAddress = PublicMessageAddress(originalPublicAddress)

        @BeforeEach
        internal fun setUp() {
            runBlocking {
                whenever(dohClient.lookUp(srvRecordName, "SRV")).thenReturn(Answer(listOf(srvRecordData)))
            }
        }

        @Test
        fun `Target host and port should be returned`() = runBlockingTest {
            val resolvedAddress = publicAddress.resolve(dohClient)

            assertEquals("https://$targetHost:$targetPort", resolvedAddress)
        }

        @Test
        fun `Any original scheme, port, path, query string or fragment should be discarded`() = runBlockingTest {
            val complexPublicAddress =
                PublicMessageAddress("http://$originalHost:9876/path?query=string#fragment")

            val resolvedAddress = complexPublicAddress.resolve(dohClient)

            assertEquals("https://$targetHost:$targetPort", resolvedAddress)
        }

        @Test
        fun `SRV data with fewer than four fields should be refused`() = runBlockingTest {
            val malformedSRVData = "1 2 3"
            whenever(dohClient.lookUp(any(), any())).thenReturn(Answer(listOf(malformedSRVData)))

            val exception = assertThrows<PublicAddressResolutionException> {
                publicAddress.resolve(dohClient)
            }

            assertEquals("Malformed SRV for $originalHost ($malformedSRVData)", exception.message)
        }

        @Test
        fun `Lookup errors should be wrapped`() = runBlockingTest {
            val lookupException = LookupFailureException("Whoops")
            whenever(dohClient.lookUp(any(), any())).thenThrow(lookupException)

            val exception = assertThrows<PublicAddressResolutionException> {
                publicAddress.resolve(dohClient)
            }

            assertEquals("Failed to resolve SRV for $originalHost", exception.message)
            assertEquals(lookupException, exception.cause)
        }
    }
}
