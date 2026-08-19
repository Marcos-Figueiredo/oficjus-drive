package br.com.oficjus.drive.di

import br.com.oficjus.drive.BuildConfig
import br.com.oficjus.drive.data.local.SessionManager
import br.com.oficjus.drive.data.remote.CnefeApi
import br.com.oficjus.drive.data.remote.NominatimApi
import br.com.oficjus.drive.data.remote.SupabaseAuthApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(sessionManager: SessionManager): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        // Interceptor que usa o token do usuário logado quando disponível
        // Se o token expirou, tenta renovar automaticamente com o refresh_token
        val authInterceptor = okhttp3.Interceptor { chain ->
            val original = chain.request()
            val token = sessionManager.accessToken
            val authToken = if (!token.isNullOrBlank()) token else BuildConfig.SUPABASE_ANON_KEY
            val request = original.newBuilder()
                .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer $authToken")
                .build()

            val response = chain.proceed(request)

            // Se recebeu 401 e temos refresh_token, tenta renovar
            if (response.code == 401 && !sessionManager.refreshToken.isNullOrBlank()) {
                response.close()
                val novoToken = runBlocking { refreshToken(sessionManager) }
                if (novoToken != null) {
                    val retryRequest = original.newBuilder()
                        .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                        .header("Authorization", "Bearer $novoToken")
                        .build()
                    return@Interceptor chain.proceed(retryRequest)
                }
            }

            response
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private suspend fun refreshToken(sessionManager: SessionManager): String? {
        return try {
            val url = URL("${BuildConfig.SUPABASE_URL}/auth/v1/token?grant_type=refresh_token")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
            conn.setRequestProperty("Content-Type", "application/json")
            val body = JSONObject().apply {
                put("refresh_token", sessionManager.refreshToken)
            }
            conn.outputStream.write(body.toString().toByteArray())

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                conn.disconnect()
                val json = JSONObject(response)
                val novoToken = json.optString("access_token", null)
                val novoRefresh = json.optString("refresh_token", null)
                if (novoToken != null) {
                    sessionManager.accessToken = novoToken
                    if (novoRefresh != null) sessionManager.refreshToken = novoRefresh
                    return novoToken
                }
            }
            conn.disconnect()
            null
        } catch (_: Exception) {
            null
        }
    }

    @Provides
    @Singleton
    fun provideCnefeApi(client: OkHttpClient): CnefeApi {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.SUPABASE_URL + "/rest/v1/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CnefeApi::class.java)
    }

    @Provides
    @Singleton
    fun provideSupabaseAuthApi(client: OkHttpClient): SupabaseAuthApi {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.SUPABASE_URL + "/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SupabaseAuthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideNominatimApi(): NominatimApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "OficJusDrive/1.0 (oficjusdrive@gmail.com)")
                    .build()
                chain.proceed(request)
            }.build()

        return Retrofit.Builder()
            .baseUrl("https://nominatim.openstreetmap.org/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NominatimApi::class.java)
    }
}