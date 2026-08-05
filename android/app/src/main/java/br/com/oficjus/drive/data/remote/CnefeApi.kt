package br.com.oficjus.drive.data.remote

import br.com.oficjus.drive.data.remote.dto.CnefeEnderecoDto
import retrofit2.http.GET
import retrofit2.http.Query

interface CnefeApi {

    @GET("cnefe_enderecos")
    suspend fun buscarPorLogradouro(
        @Query("logradouro_completo", encoded = true) logradouro: String,
        @Query("estado") estado: String = "eq.31",
        @Query("select") select: String = "cep,logradouro_tipo,logradouro_titulo,logradouro_nome,bairro,cidade,estado,latitude,longitude,numero",
        @Query("limit") limit: Int = 20
    ): List<CnefeEnderecoDto>

    @GET("cnefe_enderecos")
    suspend fun buscarPorCepENumero(
        @Query("cep") cep: String,
        @Query("numero", encoded = true) numero: String,
        @Query("select") select: String = "cep,logradouro_tipo,logradouro_titulo,logradouro_nome,bairro,cidade,estado,latitude,longitude,numero",
        @Query("limit") limit: Int = 5
    ): List<CnefeEnderecoDto>

    @GET("cnefe_enderecos")
    suspend fun buscarPorCep(
        @Query("cep") cep: String,
        @Query("select") select: String = "cep,logradouro_tipo,logradouro_titulo,logradouro_nome,bairro,cidade,estado,latitude,longitude,numero",
        @Query("limit") limit: Int = 5
    ): List<CnefeEnderecoDto>

    @GET("cnefe_enderecos")
    suspend fun buscarCoordsPorCep(
        @Query("cep") cep: String,
        @Query("select") select: String = "latitude,longitude,logradouro_tipo,logradouro_titulo,logradouro_nome,bairro,cidade",
        @Query("limit") limit: Int = 1
    ): List<CnefeEnderecoDto>
}