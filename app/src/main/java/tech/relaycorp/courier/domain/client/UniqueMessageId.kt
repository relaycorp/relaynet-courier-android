package tech.relaycorp.courier.domain.client

import tech.relaycorp.courier.data.model.MessageId
import tech.relaycorp.courier.data.model.PrivateMessageAddress

data class UniqueMessageId(
    val senderPrivateAddress: PrivateMessageAddress,
    val messageId: MessageId
) {

    val value get() = senderPrivateAddress.value + SEPARATOR + messageId.value

    companion object {
        private const val SEPARATOR = "+"
        fun from(value: String): UniqueMessageId {
            if (value.indexOf(SEPARATOR) == -1) {
                throw IllegalArgumentException("UniqueMessageId value is missing the separator $SEPARATOR")
            }

            val parts = value.split(SEPARATOR)
            return UniqueMessageId(PrivateMessageAddress(parts[0]), MessageId(parts[1]))
        }
    }
}
