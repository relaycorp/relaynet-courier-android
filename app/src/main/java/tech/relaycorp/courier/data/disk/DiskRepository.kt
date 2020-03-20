package tech.relaycorp.courier.data.disk

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
// TODO: Avoid letting exceptions go through this class, and handle them inside its methods
class DiskRepository
@Inject constructor(
    private val context: Context
) {

    private val messagesDir by lazy {
        File(context.filesDir, MESSAGE_FOLDER_NAME).also {
            if (!it.exists()) it.mkdir()
        }
    }

    suspend fun writeMessage(message: ByteArray) =
        withContext(Dispatchers.IO) {
            val file = File.createTempFile(MESSAGE_FILE_PREFIX, "", messagesDir)
            file.writeBytes(message)
            file.name
        }

    // TODO: Avoid through exceptions by handling here
    fun readMessage(path: String): InputStream =
        File(messagesDir, path).inputStream()

    suspend fun deleteMessage(path: String) {
        withContext(Dispatchers.IO) {
            File(messagesDir, path).delete()
        }
    }

    companion object {
        private const val MESSAGE_FOLDER_NAME = "messages"
        private const val MESSAGE_FILE_PREFIX = "message_"
    }
}
