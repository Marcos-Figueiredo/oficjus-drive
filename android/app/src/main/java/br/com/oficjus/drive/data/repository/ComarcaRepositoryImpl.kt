package br.com.oficjus.drive.data.repository

import br.com.oficjus.drive.domain.repository.ComarcaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton
import br.com.oficjus.drive.BuildConfig

@Singleton
class ComarcaRepositoryImpl @Inject constructor(
    private val client: OkHttpClient
) : ComarcaRepository {

    override suspend fun listarCidades(segmentoCodigo: String, tribunalCodigo: String, comarcaCodigo: String): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                val url = "${BuildConfig.SUPABASE_URL}/rest/v1/cnj_comarcas_cidades?segmento_codigo=eq.$segmentoCodigo&tribunal_codigo=eq.$tribunalCodigo&comarca_oooo=eq.$comarcaCodigo&select=cidade"
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
                    .build()
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                response.close()

                if (body.isNullOrBlank() || body == "[]") return@withContext emptyList()

                val cidadesJson = JSONArray(body)
                val cidades = mutableListOf<String>()
                for (i in 0 until cidadesJson.length()) {
                    val nome = cidadesJson.getJSONObject(i).optString("cidade", "").uppercase()
                    if (nome.isNotBlank()) cidades.add(nome)
                }
                cidades.distinct()
            } catch (_: Exception) { emptyList() }
        }
    }
}