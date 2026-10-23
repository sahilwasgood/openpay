package openpay.app.data

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Encrypted local ledger. One JSON array of payments inside an EncryptedFile
 * keyed by an Android Keystore master key. No server, no sync, no export
 * without the user asking for it.
 */
class Ledger(context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val appContext: Context = context.applicationContext
    private val file = File(appContext.filesDir, "ledger.openpay")
    private val masterKey = MasterKey.Builder(appContext)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private fun encryptedFile(): EncryptedFile =
        EncryptedFile.Builder(
            appContext,
            file,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

    @Synchronized
    fun readAll(): List<Payment> = runCatching {
        if (!file.exists()) return emptyList()
        encryptedFile().openFileInput().bufferedReader().use { it.readText() }
    }.map { json.decodeFromString<List<Payment>>(it) }.getOrDefault(emptyList())

    @Synchronized
    fun writeAll(payments: List<Payment>) {
        if (file.exists()) file.delete()
        encryptedFile().openFileOutput().use { it.write(json.encodeToString(payments).toByteArray()) }
    }
}
