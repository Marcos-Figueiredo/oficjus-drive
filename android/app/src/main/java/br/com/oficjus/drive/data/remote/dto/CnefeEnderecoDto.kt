package br.com.oficjus.drive.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CnefeEnderecoDto(
    @SerializedName("cep") val cep: String,
    @SerializedName("logradouro_tipo") val logradouroTipo: String?,
    @SerializedName("logradouro_titulo") val logradouroTitulo: String?,
    @SerializedName("logradouro_nome") val logradouroNome: String?,
    @SerializedName("bairro") val bairro: String?,
    @SerializedName("cidade") val cidade: String,
    @SerializedName("estado") val estado: String,
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("numero") val numero: String?
)

data class ViaCepResponse(
    @SerializedName("cep") val cep: String?,
    @SerializedName("logradouro") val logradouro: String?,
    @SerializedName("complemento") val complemento: String?,
    @SerializedName("bairro") val bairro: String?,
    @SerializedName("localidade") val localidade: String?,
    @SerializedName("uf") val uf: String?,
    @SerializedName("ibge") val ibge: String?,
    @SerializedName("erro") val erro: Boolean = false
)