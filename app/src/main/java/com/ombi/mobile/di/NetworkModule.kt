package com.ombi.mobile.di

import com.ombi.mobile.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.ombi.mobile.data.api.OmbiApiService
import com.ombi.mobile.data.auth.AuthManager
import com.ombi.mobile.data.auth.SessionEvent
import com.ombi.mobile.data.preferences.UserPreferences
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt module that provides the singleton network stack.
 *
 * Key design decisions:
 * - **Dynamic base URL**: Retrofit is initialised with a placeholder (`http://localhost/`).
 *   `dynamicUrlInterceptor` rewrites the host/port on every request using the value stored
 *   in [UserPreferences], so the user can change the server URL without restarting the app.
 * - **Auth injection**: `authInterceptor` adds the `Authorization: Bearer <token>` header
 *   to every request automatically; individual call sites need no manual header handling.
 * - **Moshi + KotlinJsonAdapterFactory**: Enables reflection-based adapters for data classes
 *   that don't have `@JsonClass(generateAdapter = true)`, and supports nullable Kotlin types.
 */
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
        // Attach Bearer token to every request, and treat a 401 on an
        // authenticated request as an expired/revoked session.
        val authInterceptor = Interceptor { chain ->
            val token = authManager.getToken()
            val hadToken = !token.isNullOrBlank()
            val request = if (hadToken) {
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            } else {
                chain.request()
            }
            val response = chain.proceed(request)
            // A 401 only means "session over" if we actually sent a token. The
            // login call carries none, so its 401 (bad credentials) is left for
            // the caller to surface rather than ending a session that never began.
            if (response.code == 401 && hadToken) {
                authManager.endSession(SessionEvent.EXPIRED)
            }
            response
        }

        // Rewrite the host/port to the user-configured server URL on every request.
        // Runs on an OkHttp thread (not main thread), so runBlocking is safe here.
        val dynamicUrlInterceptor = Interceptor { chain ->
            val serverUrl = userPreferences.getServerUrlSync()
            val parsed = serverUrl.toHttpUrlOrNull()
            if (parsed == null) {
                // A blank or unparseable URL previously fell through to the
                // http://localhost/ placeholder, surfacing config errors as confusing
                // connection-refused failures. Fail loudly with an actionable message.
                throw IOException("No Ombi server URL configured. Please set one in Settings.")
            } else {
                // Prepend any path prefix from the configured URL (e.g. a reverse-proxy
                // subpath like "/ombi") so requests route to <host>/ombi/api/... rather
                // than dropping the prefix and 404ing at <host>/api/...
                val prefix = parsed.encodedPath.trimEnd('/')
                val original = chain.request().url
                val newUrl = original.newBuilder()
                    .scheme(parsed.scheme)
                    .host(parsed.host)
                    .port(parsed.port)
                    .encodedPath(prefix + original.encodedPath)
                    .build()
                chain.proceed(chain.request().newBuilder().url(newUrl).build())
            }
        }

        val builder = OkHttpClient.Builder()
            // Guard against a stalled or unreachable server hanging the app indefinitely.
            // OkHttp's default per-byte read timeout does not protect against a server that
            // streams headers then stalls the body, so callTimeout bounds the whole call.
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            .addInterceptor(dynamicUrlInterceptor)
            .addInterceptor(authInterceptor)

        // Only log in debug builds — Level.BASIC logs request URLs (including search
        // terms), which are readable by other apps holding READ_LOGS on older devices.
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
            )
        }

        return builder.build()
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
