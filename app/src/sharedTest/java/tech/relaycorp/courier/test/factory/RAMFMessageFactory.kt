package tech.relaycorp.courier.test.factory

import tech.relaycorp.relaynet.issueGatewayCertificate
import tech.relaycorp.relaynet.messages.Cargo
import tech.relaycorp.relaynet.messages.CargoCollectionAuthorization
import tech.relaycorp.relaynet.wrappers.generateRSAKeyPair
import java.time.ZonedDateTime

object RAMFMessageFactory {
    const val recipientAddress = "https://example.com"
    val senderKeyPair = generateRSAKeyPair()
    val senderCertificate = issueGatewayCertificate(
        senderKeyPair.public,
        senderKeyPair.private,
        ZonedDateTime.now().plusMinutes(10),
        validityStartDate = ZonedDateTime.now().minusSeconds(5)
    )

    fun buildCargoSerialized() = Cargo(recipientAddress, "".toByteArray(), senderCertificate)
        .serialize(senderKeyPair.private)

    fun buildCCASerialized() =
        CargoCollectionAuthorization(recipientAddress, "".toByteArray(), senderCertificate)
            .serialize(senderKeyPair.private)
}
