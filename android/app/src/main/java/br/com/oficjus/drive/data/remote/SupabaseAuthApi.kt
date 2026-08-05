package br.com.oficjus.drive.data.remote

import br.com.oficjus.drive.data.remote.dto.SupabaseSignInRequest
import br.com.oficjus.drive.data.remote.dto.SupabaseSignInResponse
import br.com.oficjus.drive.data.remote.dto.SupabaseUserResponse
import retrofit2.http.*

interface SupabaseAuthApi {

    @POST("auth/v1/token?grant_type=password")
    suspend fun signIn(
        @Body credentials: SupabaseSignInRequest
    ): SupabaseSignInResponse

    @GET("auth/v1/user")
    suspend fun getUser(
        @Header("Authorization") token: String
    ): SupabaseUserResponse
}