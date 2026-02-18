package com.ombi.mobile.di

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.ombi.mobile.data.api.OmbiApiService
import com.ombi.mobile.data.auth.AuthManager
import com.ombi.mobile.data.preferences.UserPreferences
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authManager: AuthManager,
        userPreferences: UserPreferences
    ): OkHttpClient {
        // Attach Bearer token to every request
        val authInterceptor = Interceptor { chain ->
            val token = authManager.getToken()
            val request = if (!token.isNullOrBlank()) {
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            } else {
                chain.request()
            }
            chain.proceed(request)
        }

        // Rewrite the host/port to the user-configured server URL on every request.
        // Runs on an OkHttp thread (not main thread), so runBlocking is safe here.
        val dynamicUrlInterceptor = Interceptor { chain ->
            val serverUrl = userPreferences.getServerUrlSync()
            val parsed = serverUrl.toHttpUrlOrNull()
            if (parsed == null) {
                chain.proceed(chain.request())
            } else {
                val newUrl = chain.request().url.newBuilder()
                    .scheme(parsed.scheme)
                    .host(parsed.host)
                    .port(parsed.port)
                    .build()
                chain.proceed(chain.request().newBuilder().url(newUrl).build())
            }
        }

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        return OkHttpClient.Builder()
            .addInterceptor(dynamicUrlInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder()
            // Placeholder base URL — actual host is injected per-request by dynamicUrlInterceptor
            .baseUrl("http://localhost/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    @Provides
    @Singleton
    fun provideOmbiApiService(retrofit: Retrofit): OmbiApiService =
        retrofit.create(OmbiApiService::class.java)
}
