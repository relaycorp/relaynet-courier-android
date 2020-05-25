package tech.relaycorp.relaynet.cogrpc

import java.io.InputStream

fun InputStream.readBytesAndClose() = readBytes().also { close() }
