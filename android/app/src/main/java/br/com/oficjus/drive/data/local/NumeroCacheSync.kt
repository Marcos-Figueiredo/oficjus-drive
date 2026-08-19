package br.com.oficjus.drive.data.local

import br.com.oficjus.drive.BuildConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream
import javax.inject.Inject
import javax.inject.Singleton

data class NumeroCacheDto(
    val id: Long = 0,
    val logradouro_id: Long = 0,
    val numero: String,
    val cep: String,
    val latitude: Double?,
    val longitude: Double?,
    val cidade: String? = ""
)

data class NumeroCacheSyncDto(
    val logradouro_id: Long = 0,
    val numero: String,
    val cep: String,
    val latitude: Double?,
    val longitude: Double?,
    val cidade: String? = ""
)

@Singleton
class NumeroCacheSync @Inject constructor(
    private val numeroCacheDao: NumeroCacheDao,
    private val client: OkHttpClient
) {

    suspend fun sincronizar(cidade: String, estado: String = "MG") = withContext(Dispatchers.IO) {
        val cidadeUpper = cidade.uppercase()
        val baseUrl = BuildConfig.SUPABASE_URL
        val anonKey = BuildConfig.SUPABASE_ANON_KEY
        val select = "id,logradouro_id,numero,cep,latitude,longitude,cidade"
        val tipo = object : TypeToken<List<NumeroCacheDto>>() {}.type
        val gson = Gson()
        var ultimoId: Long = 0
        val limit = 1000
        var tentativas = 0

        while (true) {
            val filtroId = if (ultimoId > 0) "&id=gt.$ultimoId" else "&id=gt.0"
            val url = "$baseUrl/rest/v1/cnefe_numeros?cidade=eq.${cidadeUpper.replace(" ", "%20")}&select=$select$filtroId&limit=$limit"
            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $anonKey")
                .build()

            val json: String
            try {
                val response = client.newCall(request).execute()
                json = response.body?.string() ?: break
                response.close()
            } catch (e: Exception) {
                tentativas++
                if (tentativas >= 3) break
                delay(2000L * tentativas)
                continue
            }
            tentativas = 0

            if (json.isBlank()) break
            if (json.trimStart().startsWith("{")) {
                tentativas++
                if (tentativas >= 3) break
                delay(2000L * tentativas)
                continue
            }

            val pagina: List<NumeroCacheDto>? = try {
                gson.fromJson(json, tipo)
            } catch (_: Exception) { null }
            if (pagina.isNullOrEmpty()) break

            val validos = pagina.filter { it.numero.all { c -> c.isDigit() } }
            validos.map { dto ->
                NumeroCacheEntity(
                    logradouroId = dto.logradouro_id,
                    cep = dto.cep,
                    numero = dto.numero,
                    latitude = dto.latitude,
                    longitude = dto.longitude,
                    cidade = dto.cidade ?: ""
                )
            }.chunked(200).forEach { batch ->
                numeroCacheDao.inserirVarios(batch)
            }

            val maxId = pagina.maxOf { it.id }
            if (maxId <= ultimoId || pagina.size < limit) break
            ultimoId = maxId
        }
    }

    suspend fun sincronizarTudo(estado: String = "MG") = withContext(Dispatchers.IO) {
        // Limpa tudo antes de baixar de novo
        numeroCacheDao.limparTudo()

        val anonKey = BuildConfig.SUPABASE_ANON_KEY
        val gson = Gson()
        val baseUrl = "https://weaqkaaqalvpbxkxrfee.supabase.co/storage/v1/object/public/oficjus-sync/${estado.uppercase()}"

        // Baixa em até 3 partes (numeros.p1, p2, p3.ndjson.gz)
        for (parte in 1..3) {
            val url = "$baseUrl/numeros.p$parte.ndjson.gz"
            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $anonKey")
                .build()

            val response = client.newCall(request).execute()
            if (response.code == 404) break  // parte não existe, fim
            val body = response.body ?: continue
            val gzipStream = GZIPInputStream(body.byteStream())
            val reader = BufferedReader(InputStreamReader(gzipStream, "UTF-8"))

            val batch = mutableListOf<NumeroCacheEntity>()
            var linha: String?
            while (reader.readLine().also { linha = it } != null) {
                val dto = try {
                    gson.fromJson(linha, NumeroCacheSyncDto::class.java)
                } catch (_: Exception) { null } ?: continue
                if (!dto.numero.all { c -> c.isDigit() }) continue
                batch.add(
                    NumeroCacheEntity(
                        logradouroId = dto.logradouro_id,
                        cep = dto.cep,
                        numero = dto.numero,
                        latitude = dto.latitude,
                        longitude = dto.longitude,
                        cidade = dto.cidade ?: ""
                    )
                )
                if (batch.size >= 200) {
                    numeroCacheDao.inserirVarios(batch.toList())
                    batch.clear()
                }
            }
            if (batch.isNotEmpty()) {
                numeroCacheDao.inserirVarios(batch.toList())
            }
            reader.close()
            response.close()
        }
    }

    suspend fun sincronizarPorComarca(
        comarcaId: String,
        estado: String = "MG",
        tribunal: String = "TJMG"
    ) = withContext(Dispatchers.IO) {
        // Limpa tudo antes de baixar os dados da comarca
        numeroCacheDao.limparTudo()

        val anonKey = BuildConfig.SUPABASE_ANON_KEY
        val gson = Gson()
        val url = "https://weaqkaaqalvpbxkxrfee.supabase.co/storage/v1/object/public/oficjus-sync/${estado.uppercase()}/${tribunal}/${comarcaId}/numeros.ndjson.gz"
        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $anonKey")
            .build()

        val response = client.newCall(request).execute()
        val body = response.body ?: return@withContext
        val gzipStream = GZIPInputStream(body.byteStream())
        val reader = BufferedReader(InputStreamReader(gzipStream, "UTF-8"))

        val batch = mutableListOf<NumeroCacheEntity>()
        var linha: String?
        while (reader.readLine().also { linha = it } != null) {
            val dto = try {
                gson.fromJson(linha, NumeroCacheSyncDto::class.java)
            } catch (_: Exception) { null } ?: continue
            if (!dto.numero.all { c -> c.isDigit() }) continue
            batch.add(
                NumeroCacheEntity(
                    logradouroId = dto.logradouro_id,
                    cep = dto.cep,
                    numero = dto.numero,
                    latitude = dto.latitude,
                    longitude = dto.longitude,
                    cidade = dto.cidade ?: ""
                )
            )
            if (batch.size >= 200) {
                numeroCacheDao.inserirVarios(batch.toList())
                batch.clear()
            }
        }
        if (batch.isNotEmpty()) {
            numeroCacheDao.inserirVarios(batch.toList())
        }
        reader.close()
        response.close()
    }

    suspend fun precisaSincronizar(cidade: String): Boolean {
        return numeroCacheDao.contar() == 0
    }

    suspend fun contar(): Int {
        return numeroCacheDao.contar()
    }

    suspend fun deletarForaDaComarca(prefixosCep: List<String>) = withContext(Dispatchers.IO) {
        numeroCacheDao.deletarForaDaComarca(prefixosCep)
    }

    suspend fun contarDaComarca(prefixosCep: List<String>): Int {
        return numeroCacheDao.contarDaComarca(prefixosCep)
    }
}