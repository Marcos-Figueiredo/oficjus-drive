package br.com.oficjus.drive.data.repository

import br.com.oficjus.drive.BuildConfig
import br.com.oficjus.drive.data.local.SessionManager
import br.com.oficjus.drive.data.remote.SupabaseAuthApi
import br.com.oficjus.drive.data.remote.dto.SupabaseSignInRequest
import br.com.oficjus.drive.domain.model.Usuario
import br.com.oficjus.drive.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: SupabaseAuthApi,
    private val sessionManager: SessionManager
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

            val usuario = Usuario(
                id = user.id,
                email = user.email ?: email,
                plano = "drive"
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
            plano = "drive"
        )
    }

    override val isLoggedIn: Boolean
        get() = sessionManager.isLoggedIn
}