package br.com.oficjus.drive.data.repository

import br.com.oficjus.drive.BuildConfig
import br.com.oficjus.drive.data.local.SessionManager
import br.com.oficjus.drive.data.remote.SupabaseAuthApi
import br.com.oficjus.drive.data.remote.dto.SupabaseSignInRequest
import br.com.oficjus.drive.domain.model.Usuario
import br.com.oficjus.drive.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: SupabaseAuthApi,
    private val sessionManager: SessionManager,
    private val client: OkHttpClient
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<Usuario> {
        return try {
            val response = authApi.signIn(
                SupabaseSignInRequest(email = email, password = password)
            )

            if (response.error != null) {
                return Result.failure(Exception(response.errorDescription ?: response.error))
            }

            val accessToken = response.accessToken
            val user = response.user

            if (accessToken == null || user?.id == null) {
                return Result.failure(Exception("Resposta inválida do servidor"))
            }

            sessionManager.accessToken = accessToken
            sessionManager.refreshToken = response.refreshToken
            sessionManager.userId = user.id
            sessionManager.userEmail = user.email

            // Busca perfil (estado, cidade e comarca) da tabela profiles
            withContext(Dispatchers.IO) {
                buscarPerfil(accessToken)
            }

            val usuario = Usuario(
                id = user.id,
                email = user.email ?: email,
                plano = "drive",
                estado = sessionManager.userEstado,
                cidade = sessionManager.userCidade,
                comarcaId = sessionManager.userComarcaId,
                segmentoId = sessionManager.userSegmentoId,
                tribunalId = sessionManager.userTribunalId,
                usage = sessionManager.userUsage
            )

            Result.success(usuario)
        } catch (e: Exception) {
            Result.failure(Exception("Falha no login: ${e.message}"))
        }
    }

    override suspend fun logout() {
        sessionManager.clear()
    }

    override suspend fun getSession(): Usuario? {
        if (!sessionManager.isLoggedIn) return null
        return Usuario(
            id = sessionManager.userId ?: return null,
            email = sessionManager.userEmail ?: "",
            plano = "drive",
            estado = sessionManager.userEstado,
            cidade = sessionManager.userCidade,
            comarcaId = sessionManager.userComarcaId,
            segmentoId = sessionManager.userSegmentoId,
            tribunalId = sessionManager.userTribunalId,
            usage = sessionManager.userUsage
        )
    }

    private suspend fun buscarPerfil(accessToken: String) {
        try {
            val url = "${BuildConfig.SUPABASE_URL}/rest/v1/profiles?id=eq.${sessionManager.userId}&select=uf,cidade,cnj_j,cnj_tr,cnj_oooo,usage"
            val request = Request.Builder()
                .url(url)
                .get()
                .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer $accessToken")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()
            response.close()

            if (!body.isNullOrBlank() && body != "[]" && body != "null") {
                val json = JSONArray(body)
                if (json.length() > 0) {
                    val perfil = json.getJSONObject(0)
                    val uf = perfil.optString("uf", "")
                    val cidade = perfil.optString("cidade", "")
                    val segmento = perfil.optString("cnj_j", "")
                    val tribunal = perfil.optString("cnj_tr", "")
                    val comarca = perfil.optString("cnj_oooo", "")
                    val usage = perfil.optString("usage", "comarca")
                    if (uf.length == 2) sessionManager.userEstado = uf
                    if (cidade.isNotBlank()) sessionManager.userCidade = cidade
                    if (segmento.isNotBlank()) sessionManager.userSegmentoId = segmento
                    if (tribunal.isNotBlank()) sessionManager.userTribunalId = tribunal
                    if (comarca.isNotBlank()) sessionManager.userComarcaId = comarca
                    sessionManager.userUsage = usage
                }
            }
        } catch (_: Exception) {
            // Falha ao buscar perfil não impede o login
        }
    }

    override suspend fun refreshProfile() {
        val token = sessionManager.accessToken ?: return
        val userId = sessionManager.userId ?: return
        withContext(Dispatchers.IO) {
            try {
                val url = "${BuildConfig.SUPABASE_URL}/rest/v1/profiles?id=eq.$userId&select=uf,cidade,cnj_j,cnj_tr,cnj_oooo,usage"
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    .header("Authorization", "Bearer $token")
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string()
                response.close()

                if (!body.isNullOrBlank() && body != "[]" && body != "null") {
                    val json = JSONArray(body)
                    if (json.length() > 0) {
                        val perfil = json.getJSONObject(0)
                        val uf = perfil.optString("uf", "")
                        val cidade = perfil.optString("cidade", "")
                        val segmento = perfil.optString("cnj_j", "")
                        val tribunal = perfil.optString("cnj_tr", "")
                        val comarca = perfil.optString("cnj_oooo", "")
                        val usage = perfil.optString("usage", "comarca")
                        if (uf.length == 2) sessionManager.userEstado = uf
                        if (cidade.isNotBlank()) sessionManager.userCidade = cidade
                        if (segmento.isNotBlank()) sessionManager.userSegmentoId = segmento
                        if (tribunal.isNotBlank()) sessionManager.userTribunalId = tribunal
                        if (comarca.isNotBlank()) sessionManager.userComarcaId = comarca
                        sessionManager.userUsage = usage
                    }
                }
            } catch (_: Exception) { }
        }
    }

    override val isLoggedIn: Boolean
        get() = sessionManager.isLoggedIn
}