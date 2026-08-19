package br.com.oficjus.drive.data.local

import br.com.oficjus.drive.BuildConfig
import com.google.gson.Gson
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

/**
 * Sincroniza a base CNEFE unificada (NDJSON.gz) para as tabelas Room.
 *
 * Estratégia:
 * 1. Baixa o NDJSON.gz de 43 MB do Supabase Storage
 * 2. Descomprime linha a linha (streaming — sem estourar RAM)
 * 3. Para cada linha: insere logradouro em [LogradouroCacheEntity]
 *    e os números em [NumeroCacheEntity]
 *
 * Substitui [LogradouroCacheSync] e [NumeroCacheSync] como fonte primária.
 */
@Singleton
class CnefeUnificadaSync @Inject constructor(
    private val cacheDao: LogradouroCacheDao,
    private val numeroCacheDao: NumeroCacheDao,
    private val client: OkHttpClient
) {
    companion object {
        private const val BUCKET_URL =
            "https://weaqkaaqalvpbxkxrfee.supabase.co/storage/v1/object/public/cnefe-data"
        private const val TAG = "CnefeUnificadaSync"
    }

    /** Sincroniza: baixa, descomprime e popula Room. */
    suspend fun sincronizar(
        estado: String = "MG",
        comarcaId: String? = null,
        tribunal: String = "TJMG"
    ) = withContext(Dispatchers.IO) {
        // Monta URL: se tem comarca, baixa só a comarca; senão, estado inteiro
        val url = if (comarcaId != null) {
            "$BUCKET_URL/${estado.uppercase()}/${tribunal}/${comarcaId}/cnefe_unificada.ndjson.gz"
        } else {
            "$BUCKET_URL/${estado.uppercase()}/cnefe_unificada.ndjson.gz"
        }
        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            response.close()
            throw IllegalStateException("HTTP ${response.code} ao baixar $url")
        }

        val body = response.body ?: run {
            response.close()
            throw IllegalStateException("Resposta vazia do Storage")
        }

        // Limpa dados antigos
        cacheDao.limparTudo()
        numeroCacheDao.limparTudo()

        val gson = Gson()
        val gzipStream = GZIPInputStream(body.byteStream())
        val reader = BufferedReader(InputStreamReader(gzipStream, "UTF-8"))

        val batchLog = mutableListOf<LogradouroCacheEntity>()
        val batchNum = mutableListOf<NumeroCacheEntity>()
        var totalLog = 0
        var totalNum = 0

        var linha: String?
        while (reader.readLine().also { linha = it } != null) {
            val dto = try {
                gson.fromJson(linha, CnefeUnificada::class.java)
            } catch (_: Exception) { null } ?: continue

            // 1. Logradouro
            val cidadeCache = dto.cidade?.uppercase() ?: ""
            batchLog.add(
                LogradouroCacheEntity(
                    logradouroCompleto = dto.logradouro,
                    bairro = dto.bairro ?: "",
                    cep = dto.cep ?: "",
                    cidade = dto.cidade ?: "",
                    estado = dto.estado ?: "",
                    cidadeCache = cidadeCache
                )
            )
            totalLog++

            // 2. Números (extrai de numeros_por_cep)
            val numerosCep = dto.numerosPorCep?.get(dto.cep)
            if (numerosCep != null) {
                for (item in numerosCep) {
                    if (item.size < 3) continue
                    val num = (item[0] as? Number)?.toInt() ?: continue
                    val lat = (item[1] as? Number)?.toDouble() ?: continue
                    val lng = (item[2] as? Number)?.toDouble() ?: continue
                    batchNum.add(
                        NumeroCacheEntity(
                            logradouroId = dto.id.toLong(),
                            cep = dto.cep ?: "",
                            numero = num.toString(),
                            latitude = lat,
                            longitude = lng,
                            cidade = dto.cidade ?: ""
                        )
                    )
                    totalNum++
                }
            }

            // Flush a cada 200 logradouros
            if (batchLog.size >= 200) {
                cacheDao.inserirVarios(batchLog.toList())
                batchLog.clear()
            }
            if (batchNum.size >= 200) {
                numeroCacheDao.inserirVarios(batchNum.toList())
                batchNum.clear()
            }

            if (totalLog % 50000 == 0) {
                android.util.Log.d(TAG, "$totalLog logradouros, $totalNum números")
            }
        }

        // Flush final
        if (batchLog.isNotEmpty()) cacheDao.inserirVarios(batchLog.toList())
        if (batchNum.isNotEmpty()) numeroCacheDao.inserirVarios(batchNum.toList())

        reader.close()
        response.close()
        android.util.Log.d(TAG, "Sincronizado: $totalLog logradouros, $totalNum números")
    }

    /** Retorna true se o cache Room estiver vazio (precisa sincronizar). */
    suspend fun precisaSincronizar(): Boolean {
        return cacheDao.contarTudo() == 0
    }

    /** Retorna total de logradouros no cache. */
    suspend fun contarLogradouros(): Int = cacheDao.contarTudo()

    /** Retorna total de números no cache. */
    suspend fun contarNumeros(): Int = numeroCacheDao.contar()
}