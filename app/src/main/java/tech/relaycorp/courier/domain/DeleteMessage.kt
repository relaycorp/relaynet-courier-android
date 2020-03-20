package tech.relaycorp.courier.domain

import tech.relaycorp.courier.data.database.StoredMessageDao
import tech.relaycorp.courier.data.disk.DiskRepository
import tech.relaycorp.courier.data.model.MessageAddress
import tech.relaycorp.courier.data.model.MessageId
import tech.relaycorp.courier.data.model.StoredMessage
import javax.inject.Inject

class DeleteMessage
@Inject constructor(
    private val storedMessageDao: StoredMessageDao,
    private val diskRepository: DiskRepository
) {

    suspend fun delete(senderAddress: MessageAddress, messageId: MessageId) {
        delete(storedMessageDao.get(senderAddress, messageId))
    }

    suspend fun delete(message: StoredMessage) {
        storedMessageDao.delete(message)
        diskRepository.deleteMessage(message.storagePath)
    }
}
