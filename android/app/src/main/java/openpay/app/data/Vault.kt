package openpay.app.data

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import java.io.File

/** Encrypted string blob on disk, keyed by the Android Keystore. */
class Vault(context: Context, name: String) {

    private val appContext = context.applicationContext
    private val file = File(appContext.filesDir, name)
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
    fun read(): String? = runCatching {
        if (!file.exists()) return null
        encryptedFile().openFileInput().bufferedReader().use { it.readText() }
    }.getOrNull()

    @Synchronized
    fun write(value: String) {
        if (file.exists()) file.delete()
        encryptedFile().openFileOutput().use { it.write(value.toByteArray()) }
    }
}
