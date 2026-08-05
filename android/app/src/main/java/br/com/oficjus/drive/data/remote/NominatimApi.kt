package br.com.oficjus.drive.data.remote

import br.com.oficjus.drive.data.remote.dto.NominatimResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface NominatimApi {

    @GET("search")
    suspend fun buscarPorEndereco(
        @Query("street") street: String,
        @Query("city") city: String,
        @Query("state") state: String,
        @Query("postalcode") postalcode: String? = null,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 1
    ): List<NominatimResponse>
}