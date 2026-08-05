package br.com.oficjus.drive.data.remote.dto

import com.google.gson.annotations.SerializedName

data class SupabaseSignInRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class SupabaseSignInResponse(
    @SerializedName("access_token") val accessToken: String?,
    @SerializedName("refresh_token") val refreshToken: String?,
    @SerializedName("user") val user: SupabaseUser?,
    @SerializedName("error") val error: String?,
    @SerializedName("error_description") val errorDescription: String?
)

data class SupabaseUser(
    @SerializedName("id") val id: String?,
    @SerializedName("email") val email: String?
)

data class SupabaseUserResponse(
    @SerializedName("id") val id: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("user_metadata") val userMetadata: Map<String, Any>? = null
)