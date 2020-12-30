package tech.relaycorp.courier.data.model

import tech.relaycorp.doh.DoHClient
import tech.relaycorp.doh.LookupFailureException
import java.lang.Exception
import java.net.URL

sealed class MessageAddress {

    val type
        get() = when (this) {
            is PublicMessageAddress -> Type.Public
            is PrivateMessageAddress -> Type.Private
        }
    val value
        get() = when (this) {
            is PublicMessageAddress -> publicValue
            is PrivateMessageAddress -> privateValue
        }

    companion object {
        fun of(value: String) =
            if (value.contains(":")) {
                PublicMessageAddress(value)
            } else {
                PrivateMessageAddress(value)
            }
    }

    enum class Type(val value: String) {
        Public("public"), Private("private");

        companion object {
            fun fromValue(value: String) =
                values().firstOrNull { it.value == value }
                    ?: throw IllegalArgumentException("Invalid MessageAddress.Type value = $value")
        }
    }
}

data class PublicMessageAddress(val publicValue: String) : MessageAddress() {
    @Suppress("BlockingMethodInNonBlockingContext") // Needed for URL()
    @Throws(PublicAddressResolutionException::class)
    suspend fun resolve(doHClient: DoHClient): String {
        val originalURL = URL(publicValue)
        val srvName = "_rcrc._tcp.${originalURL.host}"
        val answer = try {
            doHClient.lookUp(srvName, "SRV")
        } catch (exc: LookupFailureException) {
            throw PublicAddressResolutionException("Failed to resolve SRV for ${originalURL.host}", exc)
        }
        val srvRecordData = answer.data.first()
        val recordFields = srvRecordData.split(" ")
        if (recordFields.size < 4) {
            throw PublicAddressResolutionException(
                "Malformed SRV for ${originalURL.host} ($srvRecordData)"
            )
        }
        val targetHost = recordFields[3].trimEnd('.')
        val targetPort = recordFields[2]
        return "https://$targetHost:$targetPort"
    }
}

class PublicAddressResolutionException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

data class PrivateMessageAddress(val privateValue: String) : MessageAddress()
