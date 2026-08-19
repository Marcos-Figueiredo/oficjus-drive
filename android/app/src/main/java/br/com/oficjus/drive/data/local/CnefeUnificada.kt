package br.com.oficjus.drive.data.local

import com.google.gson.annotations.SerializedName

/**
 * Registro da tabela unificada CNEFE (cnefe_unificada).
 * Estrutura do NDJSON exportado:
 * {"i":id, "l":logradouro, "b":bairro, "c":cep, "cid":cidade, "e":estado,
 *  "ext":extensao, "mn":menor_numero, "mx":maior_numero, "sg":sem_geo,
 *  "n":{CEP: [[numero, lat, lng], ...]}}
 */
data class CnefeUnificada(
    @SerializedName("i") val id: Int,
    @SerializedName("l") val logradouro: String,
    @SerializedName("b") val bairro: String? = null,
    @SerializedName("c") val cep: String? = null,
    @SerializedName("cid") val cidade: String? = null,
    @SerializedName("e") val estado: String? = null,
    @SerializedName("ext") val extensaoMetros: Double? = null,
    @SerializedName("mn") val menorNumero: Int? = null,
    @SerializedName("mx") val maiorNumero: Int? = null,
    @SerializedName("sg") val semGeo: Boolean = false,
    @SerializedName("n") val numerosPorCep: Map<String, List<List<Any>>>? = null
)

/**
 * Um número com coordenadas extraído do numeros_por_cep.
 * numeros_por_cep = {CEP: [[numero, lat, lng], ...]}
 */
data class NumeroCoordenada(
    val numero: Int,
    val latitude: Double,
    val longitude: Double
)
