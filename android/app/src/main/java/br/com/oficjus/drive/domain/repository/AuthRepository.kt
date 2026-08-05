package br.com.oficjus.drive.domain.repository

import br.com.oficjus.drive.domain.model.Usuario

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<Usuario>
    suspend fun logout()
    suspend fun getSession(): Usuario?
    val isLoggedIn: Boolean
}