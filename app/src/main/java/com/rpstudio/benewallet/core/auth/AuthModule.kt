package com.rpstudio.benewallet.core.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.rpstudio.benewallet.core.network.NetworkNames
import com.rpstudio.benewallet.core.network.baseClientBuilder
import com.rpstudio.benewallet.core.network.withLogging
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit

private const val SESSION_FILE_NAME = "session"

fun authModule(isDebug: Boolean): Module = module {
    single<AuthService> { get<Retrofit>(named(NetworkNames.PLAIN)).create(AuthService::class.java) }

    single<DataStore<Preferences>>(named(SESSION_FILE_NAME)) {
        PreferenceDataStoreFactory.create { androidContext().preferencesDataStoreFile(SESSION_FILE_NAME) }
    }

    single<TokenCipher> { KeystoreTokenCipher() }

    single<TokenStore> { DataStoreTokenStore(get(named(SESSION_FILE_NAME)), get()) }

    single<SessionManager> {
        DefaultSessionManager(
            store = get(),
            service = get(),
            cleaners = getAll(),
            crashReporter = get(),
            clock = get()
        )
    }

    // The default client and Retrofit of the app: every call carries the token and an HTTP 401 renews the session once
    single<OkHttpClient> {
        val sessionManager = get<SessionManager>()
        baseClientBuilder()
            .addInterceptor(AuthInterceptor(sessionManager))
            .authenticator(TokenAuthenticator(sessionManager))
            // Bodies are the wallet data; the Authorization header is redacted by withLogging
            .withLogging(isDebug, HttpLoggingInterceptor.Level.BODY)
            .build()
    }

    single<Retrofit> { get<Retrofit>(named(NetworkNames.PLAIN)).newBuilder().client(get<OkHttpClient>()).build() }
}
