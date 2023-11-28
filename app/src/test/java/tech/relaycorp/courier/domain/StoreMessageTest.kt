package tech.relaycorp.courier.domain

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.relaycorp.courier.data.database.StoredMessageDao
import tech.relaycorp.courier.data.disk.DiskRepository
import tech.relaycorp.courier.data.model.GatewayType
import tech.relaycorp.courier.data.model.StorageSize
import tech.relaycorp.courier.data.model.StorageUsage
import tech.relaycorp.courier.data.model.StoredMessage
import tech.relaycorp.relaynet.messages.Cargo
import tech.relaycorp.relaynet.messages.CargoCollectionAuthorization
import tech.relaycorp.relaynet.messages.Recipient
import tech.relaycorp.relaynet.testing.pki.CDACertPath
import tech.relaycorp.relaynet.testing.pki.KeyPairSet
import tech.relaycorp.relaynet.wrappers.nodeId
import java.time.ZonedDateTime

class StoreMessageTest {
    private val storedMessageDao = mock<StoredMessageDao>()
    private val diskRepository = mock<DiskRepository>()
    private val getStorageUsage = mock<GetStorageUsage>()

    private val internetAddress = "braavos.relaycorp.cloud"

    private val subject = StoreMessage(storedMessageDao, diskRepository, getStorageUsage)

    @BeforeEach
    internal fun setUp() {
        runBlocking {
            whenever(getStorageUsage.get())
                .thenReturn(StorageUsage(StorageSize(0), StorageSize(Long.MAX_VALUE)))
            whenever(diskRepository.writeMessage(any())).thenReturn("")
        }
    }

    @Test
    internal fun `store message with just enough space`() =
        runTest {
            val message = RAMFMessageFactory.buildCargoSerialized()

            val storage = StorageUsage(StorageSize.ZERO, StorageSize(message.size.toLong()))
            whenever(getStorageUsage.get()).thenReturn(storage)

            assertTrue(
                subject.storeCargo(
                    message.inputStream(),
                    GatewayType.Internet,
                ) is StoreMessage.Result.Success,
            )
            verify(diskRepository).writeMessage(any())
            verify(storedMessageDao).insert(any())
        }

    @Test
    internal fun `do not store message if no space is available`() =
        runTest {
            val noStorageSpace = StorageUsage(StorageSize.ZERO, StorageSize.ZERO)
            whenever(getStorageUsage.get()).thenReturn(noStorageSpace)

            val result =
                subject.storeCargo(
                    RAMFMessageFactory.buildCargoSerialized().inputStream(),
                    GatewayType.Internet,
                )
            assertEquals(
                StoreMessage.Result.Error.NoSpaceAvailable,
                result,
            )
            verify(diskRepository, never()).writeMessage(any())
            verify(storedMessageDao, never()).insert(any())
        }

    @Nested
    inner class StoreCargo {
        @Test
        fun `Malformed cargo should not be stored`() =
            runTest {
                val result =
                    subject.storeCargo("Not really a cargo".byteInputStream(), GatewayType.Internet)
                assertEquals(
                    StoreMessage.Result.Error.Malformed,
                    result,
                )
                verify(diskRepository, never()).writeMessage(any())
                verify(storedMessageDao, never()).insert(any())
            }

        @Test
        fun `Well-formed yet invalid cargo should not be stored`() =
            runTest {
                // Use a cargo that expired the day before
                val invalidCargo =
                    Cargo(
                        RAMFMessageFactory.recipient,
                        "payload".toByteArray(),
                        RAMFMessageFactory.senderCertificate,
                        creationDate = ZonedDateTime.now().minusDays(1),
                        ttl = 1,
                    )

                val invalidCargoSerialized =
                    invalidCargo.serialize(RAMFMessageFactory.senderKeyPair.private)
                val result =
                    subject.storeCargo(invalidCargoSerialized.inputStream(), GatewayType.Internet)

                assertEquals(
                    StoreMessage.Result.Error.Invalid,
                    result,
                )
                verify(diskRepository, never()).writeMessage(any())
                verify(storedMessageDao, never()).insert(any())
            }

        @Nested
        inner class BoundForInternetGateway {
            private val recipient =
                Recipient(
                    KeyPairSet.INTERNET_GW.public.nodeId,
                    internetAddress,
                )

            @Test
            fun `Cargo should be refused if Internet gateway of recipient is missing`() =
                runTest {
                    val invalidCargo =
                        Cargo(
                            recipient.copy(internetAddress = null),
                            "payload".toByteArray(),
                            CDACertPath.PRIVATE_GW,
                        )
                    val invalidCargoSerialized =
                        invalidCargo.serialize(KeyPairSet.PRIVATE_GW.private)

                    val result =
                        subject.storeCargo(invalidCargoSerialized.inputStream(), GatewayType.Internet)

                    assertEquals(StoreMessage.Result.Error.Invalid, result)
                    verify(diskRepository, never()).writeMessage(any())
                    verify(storedMessageDao, never()).insert(any())
                }

            @Test
            fun `Valid cargo should be stored`() =
                runTest {
                    val cargo =
                        Cargo(
                            recipient,
                            "payload".toByteArray(),
                            CDACertPath.PRIVATE_GW,
                        )
                    val cargoSerialized = cargo.serialize(KeyPairSet.PRIVATE_GW.private)

                    val result = subject.storeCargo(cargoSerialized.inputStream(), GatewayType.Internet)

                    assertTrue(result is StoreMessage.Result.Success)
                    verify(diskRepository).writeMessage(any())
                    argumentCaptor<StoredMessage> {
                        verify(storedMessageDao).insert(capture())
                        assertEquals(recipient.internetAddress, firstValue.recipientAddress)
                        assertEquals(GatewayType.Internet, firstValue.recipientType)
                    }
                }

            @Test
            fun `Delivery authorization should not be required`() =
                runTest {
                    val cargo =
                        Cargo(
                            recipient.copy(id = "${recipient.id}abc"),
                            "payload".toByteArray(),
                            CDACertPath.PRIVATE_GW,
                        )
                    val cargoSerialized = cargo.serialize(KeyPairSet.PRIVATE_GW.private)

                    val result = subject.storeCargo(cargoSerialized.inputStream(), GatewayType.Internet)

                    assertTrue(result is StoreMessage.Result.Success)
                    verify(diskRepository).writeMessage(any())
                    verify(storedMessageDao).insert(any())
                }
        }

        @Nested
        inner class BoundForPrivateGateway {
            private val recipient = Recipient(KeyPairSet.PRIVATE_GW.public.nodeId)

            @Test
            @Disabled // See https://github.com/relaycorp/relaynet-courier-android/issues/255
            fun `Unauthorized cargo should be refused`() =
                runTest {
                    val cargo =
                        Cargo(
                            recipient.copy(id = "${recipient.id}abc"),
                            "payload".toByteArray(),
                            CDACertPath.INTERNET_GW,
                        )
                    val cargoSerialized = cargo.serialize(KeyPairSet.INTERNET_GW.private)

                    val result = subject.storeCargo(cargoSerialized.inputStream(), GatewayType.Private)

                    assertEquals(StoreMessage.Result.Error.Invalid, result)
                    verify(diskRepository, never()).writeMessage(any())
                    verify(storedMessageDao, never()).insert(any())
                }

            @Test
            fun `Authorized cargo should be accepted`() =
                runTest {
                    val cargo =
                        Cargo(
                            recipient,
                            "payload".toByteArray(),
                            CDACertPath.INTERNET_GW,
                        )
                    val cargoSerialized = cargo.serialize(KeyPairSet.INTERNET_GW.private)

                    val result = subject.storeCargo(cargoSerialized.inputStream(), GatewayType.Private)

                    assertTrue(result is StoreMessage.Result.Success)
                    verify(diskRepository).writeMessage(any())
                    argumentCaptor<StoredMessage> {
                        verify(storedMessageDao).insert(capture())
                        assertEquals(recipient.id, firstValue.recipientAddress)
                        assertEquals(GatewayType.Private, firstValue.recipientType)
                    }
                }
        }
    }

    @Nested
    inner class StoreCCA {
        @Test
        fun `do not store malformed CCA`() =
            runTest {
                val result = subject.storeCCA("Not a RAMF message".toByteArray())
                assertEquals(
                    StoreMessage.Result.Error.Malformed,
                    result,
                )
                verify(diskRepository, never()).writeMessage(any())
                verify(storedMessageDao, never()).insert(any())
            }

        @Test
        fun `do not store well-formed yet invalid CCA`() =
            runTest {
                // Use a CCA that expired the day before
                val invalidCCA =
                    CargoCollectionAuthorization(
                        RAMFMessageFactory.recipient,
                        "payload".toByteArray(),
                        RAMFMessageFactory.senderCertificate,
                        creationDate = ZonedDateTime.now().minusDays(1),
                        ttl = 1,
                    )

                val result =
                    subject.storeCCA(invalidCCA.serialize(RAMFMessageFactory.senderKeyPair.private))

                assertEquals(
                    StoreMessage.Result.Error.Invalid,
                    result,
                )
                verify(diskRepository, never()).writeMessage(any())
                verify(storedMessageDao, never()).insert(any())
            }

        @Test
        fun `store valid CCA`() =
            runTest {
                val result = subject.storeCCA(RAMFMessageFactory.buildCCASerialized())

                assertTrue(result is StoreMessage.Result.Success)
                verify(diskRepository).writeMessage(any())
                argumentCaptor<StoredMessage> {
                    verify(storedMessageDao).insert(capture())
                    assertEquals(internetAddress, firstValue.recipientAddress)
                    assertEquals(GatewayType.Internet, firstValue.recipientType)
                }
            }
    }
}
