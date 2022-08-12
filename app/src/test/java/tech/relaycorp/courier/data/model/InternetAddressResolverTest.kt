package tech.relaycorp.courier.data.model

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.relaycorp.courier.domain.client.InternetAddressResolutionException
import tech.relaycorp.courier.domain.client.InternetAddressResolver
import tech.relaycorp.doh.Answer
import tech.relaycorp.doh.DoHClient
import tech.relaycorp.doh.LookupFailureException

class InternetAddressResolverTest {
    @Nested
    inner class Resolve {
        private val dohClient = mock<DoHClient>()

        private val internetAddress = "example.com"
        private val targetHost = "target.example.com"
        private val targetPort = "1357"
        private val srvRecordName = "_awala-crc._tcp.$internetAddress"
        private val srvRecordData = "0 1 $targetPort $targetHost."

        private val resolver = InternetAddressResolver(dohClient)

        @BeforeEach
        internal fun setUp() {
            runBlocking {
                whenever(dohClient.lookUp(srvRecordName, "SRV")).thenReturn(
                    Answer(
                        listOf(
                            srvRecordData
                        )
                    )
                )
            }
        }

        @Test
        fun `Target host and port should be returned`() = runTest {
            val resolvedAddress = resolver.resolve(internetAddress)

            assertEquals("https://$targetHost:$targetPort", resolvedAddress)
        }

        @Test
        fun `SRV data with fewer than four fields should be refused`() = runTest {
            val malformedSRVData = "1 2 3"
            whenever(dohClient.lookUp(any(), any())).thenReturn(Answer(listOf(malformedSRVData)))

            val exception = assertThrows<InternetAddressResolutionException> {
                resolver.resolve(internetAddress)
            }

            assertEquals(
                "Malformed SRV for $internetAddress ($malformedSRVData)",
                exception.message
            )
        }

        @Test
        fun `Lookup errors should be wrapped`() = runTest {
            val lookupException = LookupFailureException("Whoops")
            whenever(dohClient.lookUp(any(), any())).thenThrow(lookupException)

            val exception = assertThrows<InternetAddressResolutionException> {
                resolver.resolve(internetAddress)
            }

            assertEquals("Failed to resolve SRV for $internetAddress", exception.message)
            assertEquals(lookupException, exception.cause)
        }
    }
}
