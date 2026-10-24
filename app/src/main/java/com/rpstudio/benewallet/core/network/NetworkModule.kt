package com.rpstudio.benewallet.core.network

import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.module.Module
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

private const val TIMEOUT_SECONDS = 15L
private const val JSON_MEDIA_TYPE = "application/json"

val networkJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

/** Retrofit requires the base URL to end with "/" */
internal fun String.withTrailingSlash(): String = if (endsWith("/")) this else "$this/"

fun networkModule(baseUrl: String, isDebug: Boolean): Module = module {
    single { networkJson }

    single<OkHttpClient> {
        val builder = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)

        if (isDebug) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
                // Avoid sensitive headers for being recorded into the device's system logs
                redactHeader("Authorization")
            }
            builder.addInterceptor(loggingInterceptor)
        }

        builder.build()
    }

    single<Retrofit> {
        Retrofit.Builder()
            .baseUrl(baseUrl.trim().withTrailingSlash())
            .client(get())
            .addConverterFactory(get<Json>().asConverterFactory(JSON_MEDIA_TYPE.toMediaType()))
            .build()
    }
}
