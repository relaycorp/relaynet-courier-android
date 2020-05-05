package tech.relaycorp.relaynet

import java.io.InputStream

data class CargoDeliveryRequest(val localId: String, val cargoSerialized: InputStream)
