package tech.relaycorp.courier.domain

import tech.relaycorp.courier.data.database.StoredMessageDao
import javax.inject.Inject

class DeleteAllStorage
@Inject constructor(
    private val storedMessageDao: StoredMessageDao
) {
    suspend fun delete() {
        storedMessageDao.deleteAll()
    }
}
