package tech.relaycorp.courier.data.disk

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiskRepository
@Inject constructor(
    private val context: Context
) {

    @Throws(DiskException::class)
    suspend fun writeMessage(message: ByteArray): String =
        try {
            writeMessageUnhandled(message)
        } catch (e: IOException) {
            throw DiskException(e)
        }

    @Throws(MessageDataNotFoundException::class)
    suspend fun readMessage(path: String): InputStream =
        try {
            File(getOrCreateMessagesDir(), path).inputStream()
        } catch (e: FileNotFoundException) {
            throw MessageDataNotFoundException("Message data not found on path '$path'", e)
        }

    suspend fun deleteMessage(path: String) {
        withContext(Dispatchers.IO) {
            val messagesDir = getOrCreateMessagesDir()
            File(messagesDir, path).delete()
        }
    }

    private suspend fun writeMessageUnhandled(message: ByteArray) =
        withContext(Dispatchers.IO) {
            val file = createUniqueFile()
            file.writeBytes(message)
            file.name
        }

    private suspend fun getOrCreateMessagesDir() =
        withContext(Dispatchers.IO) {
            File(context.filesDir, MESSAGE_FOLDER_NAME).also {
                if (!it.exists()) it.mkdir()
            }
        }

    private suspend fun createUniqueFile() =
        withContext(Dispatchers.IO) {
            val messagesDir = getOrCreateMessagesDir()
            // The file created isn't temporary, but it ensures a unique filename
            File.createTempFile(MESSAGE_FILE_PREFIX, "", messagesDir)
        }

    companion object {
        // Warning: changing this folder name will make users lose the paths to their cargo
        private const val MESSAGE_FOLDER_NAME = "messages"
        private const val MESSAGE_FILE_PREFIX = "message_"
    }
}
