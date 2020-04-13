package tech.relaycorp.courier.domain

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.relaycorp.courier.data.database.StoredMessageDao
import tech.relaycorp.courier.test.factory.StoredMessageFactory

internal class DeleteExpiredMessagesTest {

    private val storedMessageDao = mock<StoredMessageDao>()
    private val deleteMessage = mock<DeleteMessage>()
    private val subject = DeleteExpiredMessages(storedMessageDao, deleteMessage)

    @Test
    internal fun delete() = runBlockingTest {
        val message = StoredMessageFactory.build()
        whenever(storedMessageDao.getExpiredBy(any())).thenReturn(listOf(message))

        val result = subject.delete()

        verify(deleteMessage).delete(eq(message))
        assertEquals(
            result,
            listOf(message)
        )
    }
}
