package tech.relaycorp.courier.domain

import tech.relaycorp.courier.data.database.StoredMessageDao
import tech.relaycorp.courier.data.disk.DiskRepository
import tech.relaycorp.courier.data.model.MessageId
import tech.relaycorp.courier.data.model.StoredMessage
import javax.inject.Inject

class DeleteMessage
    @Inject
    constructor(
        private val storedMessageDao: StoredMessageDao,
        private val diskRepository: DiskRepository,
    ) {
        suspend fun delete(
            senderId: String,
            messageId: MessageId,
        ) {
            delete(storedMessageDao.get(senderId, messageId))
        }

        suspend fun delete(message: StoredMessage) {
            storedMessageDao.delete(message)
            diskRepository.deleteMessage(message.storagePath)
        }
    }
