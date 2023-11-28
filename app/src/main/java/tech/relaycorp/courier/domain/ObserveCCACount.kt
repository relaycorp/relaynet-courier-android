package tech.relaycorp.courier.domain

import tech.relaycorp.courier.data.database.StoredMessageDao
import tech.relaycorp.courier.data.model.MessageType
import javax.inject.Inject

class ObserveCCACount
    @Inject
    constructor(
        private val storedMessageDao: StoredMessageDao,
    ) {
        fun observe() = storedMessageDao.observeCountByMessageType(MessageType.CCA)
    }
