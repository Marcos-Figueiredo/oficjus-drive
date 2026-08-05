package br.com.oficjus.drive.data.remote.dto

import com.google.gson.annotations.SerializedName

data class NominatimResponse(
    @SerializedName("lat") val lat: String?,
    @SerializedName("lon") val lon: String?,
    @SerializedName("display_name") val displayName: String?,
    @SerializedName("addresstype") val addressType: String?,
    @SerializedName("type") val type: String?
)