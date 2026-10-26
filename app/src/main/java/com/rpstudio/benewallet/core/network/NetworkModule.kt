package com.rpstudio.benewallet.core.network

import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

private const val TIMEOUT_SECONDS = 15L
private const val JSON_MEDIA_TYPE = "application/json"

val networkJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

object NetworkNames {
    /** The client and Retrofit that never carry a token. Only the sign-in, renew and sign-out calls use them. */
    const val UNAUTHENTICATED = "unauthenticated"
}

/** Retrofit requires the base URL to end with "/" */
internal fun String.withTrailingSlash(): String = if (endsWith("/")) this else "$this/"

fun baseClientBuilder(): OkHttpClient.Builder = OkHttpClient.Builder()
    .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
    .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
    .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)

/** Debug builds only. The Authorization header is never printed. */
fun OkHttpClient.Builder.withLogging(isDebug: Boolean, level: HttpLoggingInterceptor.Level): OkHttpClient.Builder =
    apply {
        if (isDebug) {
            addInterceptor(
                HttpLoggingInterceptor().apply {
                    this.level = level
                    // Avoid sensitive headers for being recorded into the device's system logs
                    redactHeader("Authorization")
                }
            )
        }
    }

fun networkModule(baseUrl: String, isDebug: Boolean): Module = module {
    single { networkJson }

    // This is the client of the sign-in, renew and sign-out calls, so its logs stay at HEADERS on purpose: the sign-in
    // body holds the password and the responses hold the tokens, and none of that may reach Logcat.
    // The client that carries the token (and logs BODY) is defined in authModule.
    single<OkHttpClient>(named(NetworkNames.UNAUTHENTICATED)) {
        baseClientBuilder().withLogging(isDebug, HttpLoggingInterceptor.Level.HEADERS).build()
    }

    single<Retrofit>(named(NetworkNames.UNAUTHENTICATED)) {
        Retrofit.Builder()
            .baseUrl(baseUrl.trim().withTrailingSlash())
            .client(get<OkHttpClient>(named(NetworkNames.UNAUTHENTICATED)))
            .addConverterFactory(get<Json>().asConverterFactory(JSON_MEDIA_TYPE.toMediaType()))
            .build()
    }
}
