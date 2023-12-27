package tech.relaycorp.courier.domain

import tech.relaycorp.relaynet.messages.Cargo
import tech.relaycorp.relaynet.messages.CargoCollectionAuthorization
import tech.relaycorp.relaynet.messages.Recipient
import tech.relaycorp.relaynet.testing.pki.CDACertPath
import tech.relaycorp.relaynet.testing.pki.KeyPairSet
import tech.relaycorp.relaynet.wrappers.nodeId

object RAMFMessageFactory {
    val recipient = Recipient(KeyPairSet.INTERNET_GW.public.nodeId, "braavos.relaycorp.cloud")
    val senderKeyPair = KeyPairSet.PRIVATE_GW
    val senderCertificate = CDACertPath.PRIVATE_GW

    fun buildCargoSerialized() =
        Cargo(recipient, "".toByteArray(), senderCertificate)
            .serialize(senderKeyPair.private)

    fun buildCCASerialized() =
        CargoCollectionAuthorization(recipient, "".toByteArray(), senderCertificate)
            .serialize(senderKeyPair.private)
}
