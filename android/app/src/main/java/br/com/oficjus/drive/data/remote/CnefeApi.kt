package br.com.oficjus.drive.data.remote

import br.com.oficjus.drive.data.remote.dto.CnefeEnderecoDto
import br.com.oficjus.drive.data.remote.dto.LogradouroCacheDto
import br.com.oficjus.drive.data.remote.dto.LogradouroFallbackDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface CnefeApi {

    @GET("logradouros_fallback")
    suspend fun buscarFallbackPorCidade(
        @Query("cidade") cidade: String,
        @Query("estado") estado: String,
        @Query("select") select: String = "*",
        @Query("limit") limit: Int = 5000
    ): List<LogradouroFallbackDto>

    @POST("logradouros_fallback")
    suspend fun inserirFallback(
        @Body body: LogradouroFallbackDto
    ): List<LogradouroFallbackDto>

    @GET("cnefe_logradouros")
    suspend fun buscarLogradourosPorCep(
        @Query("cep") cep: String,
        @Query("cidade") cidade: String? = null,
        @Query("select") select: String = "logradouro_completo,bairro,cep,cidade,estado",
        @Query("limit") limit: Int = 20
    ): List<LogradouroCacheDto>

    @GET("cnefe_enderecos")
    suspend fun buscarPorLogradouro(
        @Query("logradouro_completo", encoded = true) logradouroCompleto: String,
        @Query("estado") estado: String,
        @Query("cidade") cidade: String? = null,
        @Query("select") select: String = "cep,logradouro_completo,bairro,cidade,estado,latitude,longitude,numero",
        @Query("limit") limit: Int = 20
    ): List<CnefeEnderecoDto>

    @GET("cnefe_enderecos")
    suspend fun buscarPorCepENumero(
        @Query("cep") cep: String,
        @Query("numero", encoded = true) numero: String,
        @Query("select") select: String = "cep,logradouro_completo,bairro,cidade,estado,latitude,longitude,numero",
        @Query("limit") limit: Int = 5
    ): List<CnefeEnderecoDto>

    @GET("cnefe_enderecos")
    suspend fun buscarPorCep(
        @Query("cep") cep: String,
        @Query("select") select: String = "cep,logradouro_completo,bairro,cidade,estado,latitude,longitude,numero",
        @Query("limit") limit: Int = 5
    ): List<CnefeEnderecoDto>
}