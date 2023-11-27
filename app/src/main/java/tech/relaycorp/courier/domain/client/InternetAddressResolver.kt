package tech.relaycorp.courier.domain.client

import tech.relaycorp.doh.DoHClient
import tech.relaycorp.doh.LookupFailureException

class InternetAddressResolver(private val dohClient: DoHClient) {
    @Throws(InternetAddressResolutionException::class)
    suspend fun resolve(internetAddress: String): String {
        val srvName = "_awala-crc._tcp.$internetAddress"
        val answer = try {
            dohClient.lookUp(srvName, "SRV")
        } catch (exc: LookupFailureException) {
            throw InternetAddressResolutionException(
                "Failed to resolve SRV for $internetAddress",
                exc
            )
        }
        val srvRecordData = answer.data.first()
        val recordFields = srvRecordData.split(" ")
        if (recordFields.size < 4) {
            throw InternetAddressResolutionException(
                "Malformed SRV for $internetAddress ($srvRecordData)"
            )
        }
        val targetHost = recordFields[3].trimEnd('.')
        val targetPort = recordFields[2]
        return "https://$targetHost:$targetPort"
    }
}
