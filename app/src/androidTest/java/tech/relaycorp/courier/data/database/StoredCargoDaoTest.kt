package tech.relaycorp.courier.data.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import tech.relaycorp.courier.test.AppTestProvider.database
import tech.relaycorp.courier.test.factory.StoredCargoFactory

@RunWith(AndroidJUnit4::class)
class StoredCargoDaoTest {

    private lateinit var storedCargoDao: StoredCargoDao

    @Before
    fun setUp() {
        storedCargoDao = database.storedCargoDao()
    }

    @Test
    fun insertAndGet() = runBlockingTest {
        val cargo = StoredCargoFactory.build()
        storedCargoDao.insert(cargo)
        assertEquals(
            listOf(cargo),
            storedCargoDao.getAll().first()
        )
    }

    @Test
    fun getFullSize() = runBlockingTest {
        val cargo =
            (1..3)
                .map { StoredCargoFactory.build() }
                .also { it.map { c -> storedCargoDao.insert(c) } }
        assertEquals(
            cargo.map { it.size }.sum(),
            storedCargoDao.getFullSize().first()
        )
    }
}
