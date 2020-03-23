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

    suspend fun writeMessage(message: ByteArray): String =
        withContext(Dispatchers.IO) {
            val messagesDir = getOrCreateMessagesDir()
            val file = File.createTempFile(MESSAGE_FILE_PREFIX, "", messagesDir)
            file.writeBytes(message)
            file.name
        }

    // TODO: Avoid through exceptions by handling here
    suspend fun readMessage(path: String): InputStream =
        File(getOrCreateMessagesDir(), path).inputStream()

    suspend fun deleteMessage(path: String) {
        withContext(Dispatchers.IO) {
            val messagesDir = getOrCreateMessagesDir()
            File(messagesDir, path).delete()
        }
    }

    private suspend fun getOrCreateMessagesDir() =
        withContext(Dispatchers.IO) {
            File(context.filesDir, MESSAGE_FOLDER_NAME).also {
                if (!it.exists()) it.mkdir()
            }
        }

    companion object {
        // Warning: changing this folder name will make users lose the paths to their cargo
        private const val MESSAGE_FOLDER_NAME = "messages"
        private const val MESSAGE_FILE_PREFIX = "message_"
    }
}
