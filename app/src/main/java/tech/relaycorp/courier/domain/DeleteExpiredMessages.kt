package tech.relaycorp.courier.domain

import tech.relaycorp.courier.data.database.StoredMessageDao
import java.util.Date
import javax.inject.Inject

class DeleteExpiredMessages
@Inject constructor(
    private val storedMessageDao: StoredMessageDao,
    private val deleteMessage: DeleteMessage
) {

    suspend fun delete() {
        val messagesToDelete = storedMessageDao.getExpiredBy(Date())
        messagesToDelete.forEach {
            deleteMessage.delete(it)
        }
    }
}
