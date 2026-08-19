package br.com.oficjus.drive.data.local

import br.com.oficjus.drive.BuildConfig
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream
import javax.inject.Inject
import javax.inject.Singleton

/** DTO exclusivo do sync — só os campos que o SELECT pede */
data class LogradouroCacheSyncDto(
    @SerializedName("logradouro_completo") val logradouroCompleto: String,
    @SerializedName("bairro") val bairro: String?,
    @SerializedName("cep") val cep: String?,
    @SerializedName("cidade") val cidade: String?,
    @SerializedName("estado") val estado: String?
)

@Singleton
class LogradouroCacheSync @Inject constructor(
    private val cacheDao: LogradouroCacheDao,
    private val client: OkHttpClient
) {

    suspend fun sincronizar(cidade: String, estado: String = "MG") = withContext(Dispatchers.IO) {
        val cidadeUpper = cidade.uppercase()

        // Limpa dados antigos antes de sincronizar (evita duplicatas de syncs quebrados)
        cacheDao.deletarPorCidade(cidadeUpper)

        val baseUrl = BuildConfig.SUPABASE_URL
        val anonKey = BuildConfig.SUPABASE_ANON_KEY
        val select = "logradouro_completo,bairro,cep,cidade,estado"
        val order = "logradouro_completo"
        val tipo = object : TypeToken<List<LogradouroCacheSyncDto>>() {}.type
        val gson = Gson()
        var offset = 0

        while (true) {
            val url = "$baseUrl/rest/v1/cnefe_logradouros?cidade=eq.${cidadeUpper.replace(" ", "%20")}&select=$select&order=$order&limit=1000&offset=$offset"
            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $anonKey")
                .build()
            val response = client.newCall(request).execute()
            val json = response.body?.string() ?: break
            response.close()

            val pagina: List<LogradouroCacheSyncDto> = gson.fromJson(json, tipo) ?: break
            if (pagina.isEmpty()) break

            // Salva cada página imediatamente (não espera o loop terminar)
            pagina.map { dto ->
                LogradouroCacheEntity(
                    logradouroCompleto = dto.logradouroCompleto,
                    bairro = dto.bairro ?: "",
                    cep = dto.cep ?: "",
                    cidade = dto.cidade ?: "",
                    estado = dto.estado ?: "",
                    cidadeCache = cidadeUpper
                )
            }.chunked(200).forEach { batch ->
                cacheDao.inserirVarios(batch)
            }

            if (pagina.size < 1000) break
            offset += 1000
        }
    }

    suspend fun precisaSincronizar(cidade: String): Boolean {
        return cacheDao.contarPorCidade(cidade.uppercase()) == 0
    }

    suspend fun contarLogradouros(cidade: String): Int {
        return cacheDao.contarPorCidade(cidade.uppercase())
    }

    suspend fun sincronizarTudo(estado: String = "MG") = withContext(Dispatchers.IO) {
        // Limpa tudo antes de baixar de novo
        cacheDao.limparTudo()

        val anonKey = BuildConfig.SUPABASE_ANON_KEY
        val gson = Gson()
        val url = "https://weaqkaaqalvpbxkxrfee.supabase.co/storage/v1/object/public/oficjus-sync/${estado.uppercase()}/logradouros.ndjson.gz"
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

        val batch = mutableListOf<LogradouroCacheEntity>()
        var linha: String?
        while (reader.readLine().also { linha = it } != null) {
            val dto = try {
                gson.fromJson(linha, LogradouroCacheSyncDto::class.java)
            } catch (_: Exception) { null } ?: continue
            batch.add(
                LogradouroCacheEntity(
                    logradouroCompleto = dto.logradouroCompleto,
                    bairro = dto.bairro ?: "",
                    cep = dto.cep ?: "",
                    cidade = dto.cidade ?: "",
                    estado = dto.estado ?: "",
                    cidadeCache = dto.cidade?.uppercase() ?: ""
                )
            )
            if (batch.size >= 200) {
                cacheDao.inserirVarios(batch.toList())
                batch.clear()
            }
        }
        if (batch.isNotEmpty()) {
            cacheDao.inserirVarios(batch.toList())
        }
        reader.close()
        response.close()
    }

    suspend fun sincronizarPorComarca(
        comarcaId: String,
        estado: String = "MG",
        tribunal: String = "TJMG"
    ) = withContext(Dispatchers.IO) {
        // Limpa tudo antes de baixar os dados da comarca
        cacheDao.limparTudo()

        val anonKey = BuildConfig.SUPABASE_ANON_KEY
        val gson = Gson()
        val url = "https://weaqkaaqalvpbxkxrfee.supabase.co/storage/v1/object/public/oficjus-sync/${estado.uppercase()}/${tribunal}/${comarcaId}/logradouros.ndjson.gz"
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

        val batch = mutableListOf<LogradouroCacheEntity>()
        var linha: String?
        while (reader.readLine().also { linha = it } != null) {
            val dto = try {
                gson.fromJson(linha, LogradouroCacheSyncDto::class.java)
            } catch (_: Exception) { null } ?: continue
            batch.add(
                LogradouroCacheEntity(
                    logradouroCompleto = dto.logradouroCompleto,
                    bairro = dto.bairro ?: "",
                    cep = dto.cep ?: "",
                    cidade = dto.cidade ?: "",
                    estado = dto.estado ?: "",
                    cidadeCache = dto.cidade?.uppercase() ?: ""
                )
            )
            if (batch.size >= 200) {
                cacheDao.inserirVarios(batch.toList())
                batch.clear()
            }
        }
        if (batch.isNotEmpty()) {
            cacheDao.inserirVarios(batch.toList())
        }
        reader.close()
        response.close()
    }

    suspend fun deletarForaDaComarca(cidades: List<String>) = withContext(Dispatchers.IO) {
        cacheDao.deletarForaDaComarca(cidades.map { it.uppercase() })
    }

    suspend fun contarTudo(): Int {
        return cacheDao.contarTudo()
    }

    suspend fun listarCepsDasCidades(cidades: List<String>): List<String> {
        return cacheDao.listarCepsDasCidades(cidades.map { it.uppercase() })
    }
}