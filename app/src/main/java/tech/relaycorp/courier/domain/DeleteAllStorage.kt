package tech.relaycorp.courier.domain

import tech.relaycorp.courier.data.database.StoredMessageDao
import tech.relaycorp.courier.data.disk.DiskRepository
import javax.inject.Inject

class DeleteAllStorage
    @Inject
    constructor(
        private val storedMessageDao: StoredMessageDao,
        private val diskRepository: DiskRepository,
    ) {
        suspend fun delete() {
            storedMessageDao.deleteAll()
            diskRepository.deleteAllMessages()
        }
    }
