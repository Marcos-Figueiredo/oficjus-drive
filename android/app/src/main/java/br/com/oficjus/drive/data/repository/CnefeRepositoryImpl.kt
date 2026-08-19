package br.com.oficjus.drive.data.repository

import br.com.oficjus.drive.BuildConfig
import br.com.oficjus.drive.data.local.LogradouroCacheDao
import br.com.oficjus.drive.data.local.NumeroCacheDao
import br.com.oficjus.drive.data.remote.CnefeApi
import br.com.oficjus.drive.domain.Endereco
import br.com.oficjus.drive.domain.repository.CnefeRepository
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CnefeRepositoryImpl @Inject constructor(
    private val cnefeApi: CnefeApi,
    private val cacheDao: LogradouroCacheDao,
    private val numeroCacheDao: NumeroCacheDao,
    private val client: OkHttpClient
) : CnefeRepository {

    override suspend fun buscarPorLogradouro(termo: String, cidade: String?, estado: String?): List<Endereco> {
        val termoNormalizado = termo
            .uppercase()
            .replace("[ÁÀÂÃÄ]".toRegex(), "A")
            .replace("[ÉÈÊË]".toRegex(), "E")
            .replace("[ÍÌÎÏ]".toRegex(), "I")
            .replace("[ÓÒÔÕÖ]".toRegex(), "O")
            .replace("[ÚÙÛÜ]".toRegex(), "U")
            .replace("Ç", "C")
            .replace("Ñ", "N")
            .replace(Regex("[^A-Z0-9\\s]"), "")
            .trim()
        if (termoNormalizado.length < 3) return emptyList()

        val cidadeBusca = (cidade ?: "").uppercase()
        val estadoBusca = (estado ?: "MG").uppercase()

        // 1. Cache local (Room) — sempre retorna se encontrar algo
        val resultadosCache = cacheDao.buscar(termoNormalizado, cidadeBusca)
        if (resultadosCache.isNotEmpty()) {
            return resultadosCache.map { entity ->
                Endereco(
                    cep = entity.cep, logradouro = entity.logradouroCompleto, numero = "",
                    bairro = entity.bairro, cidade = entity.cidade, estado = entity.estado,
                    latitude = null, longitude = null
                )
            }.distinctBy { it.logradouro }
        }

        // 2. Tenta fallback no Supabase (logradouros_fallback)
        try {
            val fallback = cnefeApi.buscarFallbackPorCidade(
                cidade = "eq.$cidadeBusca",
                estado = "eq.$estadoBusca"
            )
            if (fallback.isNotEmpty()) {
                return fallback.map { dto ->
                    Endereco(
                        cep = dto.cep, logradouro = dto.logradouroCompleto, numero = dto.numero,
                        bairro = dto.bairro, cidade = dto.cidade, estado = dto.estado,
                        latitude = dto.latitude, longitude = dto.longitude
                    )
                }.distinctBy { it.logradouro }
            }
        } catch (_: Exception) { }

        // 3. Fallback PostgREST direto
        return try {
            val filterValue = "like.*${termoNormalizado}*"
            val response = cnefeApi.buscarPorLogradouro(
                logradouroCompleto = filterValue.replace(" ", "%20"),
                estado = "eq.MG",
                cidade = "eq.$cidadeBusca"
            )
            response
                .filter { it.cep.isNotBlank() }
                .groupBy { it.logradouroCompleto + "|" + it.cep }
                .map { (_, items) -> items.first() }
                .take(20)
                .map { dto ->
                    Endereco(
                        cep = dto.cep, logradouro = dto.logradouroCompleto ?: "", numero = "",
                        bairro = dto.bairro ?: "", cidade = dto.cidade, estado = dto.estado,
                        latitude = dto.latitude, longitude = dto.longitude
                    )
                }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun buscarPorCepENumero(cep: String, numero: String): List<Endereco> {
        return try {
            // 1. Cache local da filha (numero_cache) — offline e rápido
            val cacheNumero = numeroCacheDao.buscarPorCepENumero(cep, numero)
            if (cacheNumero != null) {
                val logradouro = cacheDao.buscarPorCep(cep).firstOrNull()
                return listOf(
                    Endereco(
                        cep = cep,
                        logradouro = logradouro?.logradouroCompleto ?: "",
                        numero = numero,
                        bairro = logradouro?.bairro ?: "",
                        cidade = logradouro?.cidade ?: "",
                        estado = logradouro?.estado ?: "MG",
                        latitude = cacheNumero.latitude,
                        longitude = cacheNumero.longitude
                    )
                )
            }

            // 2. Cache local da mãe (logradouro_cache) — só se não tiver número
            val cacheResults = cacheDao.buscarPorCep(cep)
            if (cacheResults.isNotEmpty() && numero.isBlank()) {
                return cacheResults.map { entity ->
                    Endereco(
                        cep = entity.cep, logradouro = entity.logradouroCompleto, numero = "",
                        bairro = entity.bairro, cidade = entity.cidade, estado = entity.estado
                    )
                }
            }

            // 3. Fallback: PostgREST direto via view cnefe_enderecos
            val filtroCep = "eq.$cep"
            val response = if (numero.isNotBlank()) {
                cnefeApi.buscarPorCepENumero(filtroCep, "eq.$numero")
            } else {
                cnefeApi.buscarPorCep(filtroCep)
            }
            response.map { dto ->
                Endereco(
                    cep = dto.cep, logradouro = dto.logradouroCompleto ?: "",
                    numero = dto.numero ?: numero, bairro = dto.bairro ?: "",
                    cidade = dto.cidade, estado = dto.estado,
                    latitude = dto.latitude, longitude = dto.longitude
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    override suspend fun buscarPorCep(cep: String): List<Endereco> {
        return try {
            // 1. Cache local primeiro
            val cacheResults = cacheDao.buscarPorCep(cep)
            if (cacheResults.isNotEmpty()) {
                return cacheResults.map { entity ->
                    Endereco(
                        cep = entity.cep, logradouro = entity.logradouroCompleto, numero = "",
                        bairro = entity.bairro, cidade = entity.cidade, estado = entity.estado
                    )
                }
            }

            // 2. Fallback: PostgREST em cnefe_logradouros
            val response = cnefeApi.buscarLogradourosPorCep("eq.$cep")
            response.map { dto ->
                Endereco(
                    cep = dto.cep, logradouro = dto.logradouroCompleto, numero = "",
                    bairro = dto.bairro, cidade = dto.cidade, estado = dto.estado
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    override suspend fun buscarNumerosPorCep(cep: String): List<Endereco> {
        return numeroCacheDao.buscarPorCep(cep).map { entity ->
            val logradouro = cacheDao.buscarPorCep(cep).firstOrNull()?.logradouroCompleto ?: ""
            Endereco(
                cep = entity.cep,
                logradouro = logradouro,
                numero = entity.numero,
                bairro = "",
                cidade = "",
                estado = "MG",
                latitude = entity.latitude,
                longitude = entity.longitude
            )
        }
    }

    override suspend fun upsertCoordenadas(cep: String, numero: String, latitude: Double, longitude: Double, cidade: String, logradouroId: Long) {
        val baseUrl = BuildConfig.SUPABASE_URL
        val anonKey = BuildConfig.SUPABASE_ANON_KEY
        val json = """{"latitude":$latitude,"longitude":$longitude,"cidade":"${cidade}","logradouro_id":$logradouroId}"""

        // Tenta PATCH primeiro (se (cep,numero) já existe)
        val patchUrl = "$baseUrl/rest/v1/cnefe_numeros?cep=eq.${cep}&numero=eq.${numero}"
        val patchBody = """{"latitude":$latitude,"longitude":$longitude}""".toRequestBody("application/json".toMediaType())
        val patchRequest = Request.Builder()
            .url(patchUrl)
            .patch(patchBody)
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $anonKey")
            .addHeader("Prefer", "return=minimal")
            .build()

        try {
            val response = client.newCall(patchRequest).execute()
            if (response.code in 200..204) {
                response.close()
                return  // PATCH funcionou, registro já existe e foi atualizado
            }
            response.close()
        } catch (_: Exception) { }

        // Se PATCH não afetou nenhuma linha, faz POST (INSERT)
        val postBody = """{"cep":"$cep","numero":"$numero","latitude":$latitude,"longitude":$longitude,"cidade":"${cidade}","logradouro_id":$logradouroId}""".toRequestBody("application/json".toMediaType())
        val postRequest = Request.Builder()
            .url("$baseUrl/rest/v1/cnefe_numeros")
            .post(postBody)
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $anonKey")
            .addHeader("Prefer", "return=minimal")
            .build()

        try {
            client.newCall(postRequest).execute().close()
        } catch (_: Exception) { }
    }
}
