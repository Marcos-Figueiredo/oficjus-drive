package br.com.oficjus.drive.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences

    init {
        val p = try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "oficjus_drive_session",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Exception) {
            // Fallback para SharedPreferences comum se a criptografia falhar
            context.getSharedPreferences("oficjus_drive_session_plain", Context.MODE_PRIVATE)
        }
        prefs = p
    }

    var accessToken: String?
        get() = prefs.getString(KEY_ACCESS_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_ACCESS_TOKEN, value).apply()

    var refreshToken: String?
        get() = prefs.getString(KEY_REFRESH_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_REFRESH_TOKEN, value).apply()

    var userId: String?
        get() = prefs.getString(KEY_USER_ID, null)
        set(value) = prefs.edit().putString(KEY_USER_ID, value).apply()

    var userEmail: String?
        get() = prefs.getString(KEY_USER_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_USER_EMAIL, value).apply()

    var userEstado: String?
        get() = prefs.getString(KEY_USER_ESTADO, null)
        set(value) = prefs.edit().putString(KEY_USER_ESTADO, value).apply()

    var userCidade: String?
        get() = prefs.getString(KEY_USER_CIDADE, null)
        set(value) = prefs.edit().putString(KEY_USER_CIDADE, value).apply()

    var userComarcaId: String?
        get() = prefs.getString(KEY_USER_COMARCA, null)
        set(value) = prefs.edit().putString(KEY_USER_COMARCA, value).apply()

    var userSegmentoId: String?
        get() = prefs.getString(KEY_USER_SEGMENTO, null)
        set(value) = prefs.edit().putString(KEY_USER_SEGMENTO, value).apply()

    var userTribunalId: String?
        get() = prefs.getString(KEY_USER_TRIBUNAL, null)
        set(value) = prefs.edit().putString(KEY_USER_TRIBUNAL, value).apply()

    var userUsage: String?
        get() = prefs.getString(KEY_USER_USAGE, null)
        set(value) = prefs.edit().putString(KEY_USER_USAGE, value).apply()

    val isLoggedIn: Boolean
        get() = accessToken != null && userId != null

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_ESTADO = "user_estado"
        private const val KEY_USER_CIDADE = "user_cidade"
        private const val KEY_USER_COMARCA = "user_comarca"
        private const val KEY_USER_SEGMENTO = "user_segmento"
        private const val KEY_USER_TRIBUNAL = "user_tribunal"
        private const val KEY_USER_USAGE = "user_usage"
    }
}