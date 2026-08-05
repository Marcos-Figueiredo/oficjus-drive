package br.com.oficjus.drive.di

import br.com.oficjus.drive.BuildConfig
import br.com.oficjus.drive.data.remote.CnefeApi
import br.com.oficjus.drive.data.remote.NominatimApi
import br.com.oficjus.drive.data.remote.SupabaseAuthApi
import br.com.oficjus.drive.data.remote.ViaCepApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        // Interceptor para autenticação do Supabase
        val authInterceptor = okhttp3.Interceptor { chain ->
            val original = chain.request()
            val request = original.newBuilder()
                .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
                .build()
            chain.proceed(request)
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
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
    fun provideViaCepApi(client: OkHttpClient): ViaCepApi {
        return Retrofit.Builder()
            .baseUrl("https://viacep.com.br/ws/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ViaCepApi::class.java)
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