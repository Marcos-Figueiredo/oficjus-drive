package br.com.oficjus.drive.data.remote

import br.com.oficjus.drive.data.remote.dto.ViaCepResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface ViaCepApi {

    @GET("{cep}/json/")
    suspend fun buscarPorCep(@Path("cep") cep: String): ViaCepResponse
}