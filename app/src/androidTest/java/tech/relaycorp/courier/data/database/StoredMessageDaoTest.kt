package tech.relaycorp.courier.data.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import tech.relaycorp.courier.test.AppTestProvider.database
import tech.relaycorp.courier.test.factory.StoredMessageFactory

@RunWith(AndroidJUnit4::class)
class StoredMessageDaoTest {

    private lateinit var storedMessageDao: StoredMessageDao

    @Before
    fun setUp() {
        storedMessageDao = database.storedMessageDao()
    }

    @Test
    fun insertAndGet() = runBlockingTest {
        val messages = StoredMessageFactory.build()
        storedMessageDao.insert(messages)
        assertEquals(
            listOf(messages),
            storedMessageDao.observeAll().first()
        )
    }

    @Test
    fun getFullSize() = runBlockingTest {
        assertEquals(
            0L,
            storedMessageDao.observeTotalSize().first()
        )

        val messages =
            (1..3)
                .map { StoredMessageFactory.build() }
                .also { it.map { c -> storedMessageDao.insert(c) } }
        assertEquals(
            messages.map { it.size.bytes }.sum(),
            storedMessageDao.observeTotalSize().first().bytes
        )
    }
}
