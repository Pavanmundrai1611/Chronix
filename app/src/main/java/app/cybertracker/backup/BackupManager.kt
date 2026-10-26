package app.cybertracker.backup

import android.content.ContentResolver
import android.net.Uri
import app.cybertracker.data.repository.EntryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream

/** Reads/writes backup files through user-chosen Storage Access Framework URIs only. */
class BackupManager(
    private val resolver: ContentResolver,
    private val repository: EntryRepository,
) {
    /** Returns the total number of exported entries, lists and reminders. */
    suspend fun export(uri: Uri): Int = withContext(Dispatchers.IO) {
        val entries = repository.snapshot()
        val lists = repository.snapshotLists()
        val reminders = repository.snapshotReminders()
        val text = BackupJson.encode(entries, System.currentTimeMillis(), lists, reminders)
        val out = resolver.openOutputStream(uri, "wt") ?: throw BackupException("Could not open the destination file.")
        out.use { it.write(text.toByteArray(Charsets.UTF_8)) }
        entries.size + lists.size + reminders.size
    }

    /** Validates the entire file first; the database is only touched if it is fully valid. */
    suspend fun restore(uri: Uri): Int = withContext(Dispatchers.IO) {
        val input = resolver.openInputStream(uri) ?: throw BackupException("Could not open the selected file.")
        val bytes = input.use { readLimited(it) }
        val data = BackupJson.decode(String(bytes, Charsets.UTF_8))
        repository.replaceAll(data)
        data.entries.size + (data.lists?.size ?: 0) + (data.reminders?.size ?: 0)
    }

    private fun readLimited(input: InputStream): ByteArray {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(8192)
        var total = 0
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            total += n
            if (total > BackupJson.MAX_BYTES) throw BackupException("The backup file is too large.")
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }
}
