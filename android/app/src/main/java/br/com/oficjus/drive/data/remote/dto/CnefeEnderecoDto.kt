package br.com.oficjus.drive.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CnefeEnderecoDto(
    @SerializedName("cep") val cep: String,
    @SerializedName("logradouro_completo") val logradouroCompleto: String?,
    @SerializedName("bairro") val bairro: String?,
    @SerializedName("cidade") val cidade: String,
    @SerializedName("estado") val estado: String,
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("numero") val numero: String?
)

/** DTO para o cache local (cnefe_logradouros - mãe) */
data class LogradouroCacheDto(
    @SerializedName("logradouro_completo") val logradouroCompleto: String,
    @SerializedName("bairro") val bairro: String,
    @SerializedName("cep") val cep: String,
    @SerializedName("cidade") val cidade: String,
    @SerializedName("estado") val estado: String
)

/** DTO para a tabela logradouros_fallback */
data class LogradouroFallbackDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("logradouro_completo") val logradouroCompleto: String,
    @SerializedName("bairro") val bairro: String,
    @SerializedName("numero") val numero: String,
    @SerializedName("cep") val cep: String,
    @SerializedName("cidade") val cidade: String,
    @SerializedName("estado") val estado: String,
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?
)