package com.rpstudio.benewallet.core.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import java.time.Instant
import java.util.Base64
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

private val sessionKey = stringPreferencesKey("session")

internal interface TokenStore {
    /** The saved tokens, or null when there are none, or they can no longer be read. */
    suspend fun read(): AuthTokens?

    suspend fun save(tokens: AuthTokens)

    suspend fun clear()
}

@Serializable
private data class StoredTokens(val accessToken: String, val refreshToken: String, val accessExpiresAtMillis: Long)

/** Tokens are serialized, encrypted by [cipher] and only then written to the DataStore as a Base64 string. */
internal class DataStoreTokenStore(
    private val dataStore: DataStore<Preferences>,
    private val cipher: TokenCipher,
    private val json: Json = Json
) : TokenStore {
    override suspend fun read(): AuthTokens? {
        val encoded = dataStore.data.first()[sessionKey]
        val plain = encoded?.let { cipher.decrypt(Base64.getDecoder().decode(it)) }
        return plain?.let(::parse)
    }

    private fun parse(plain: ByteArray): AuthTokens? = try {
        json.decodeFromString<StoredTokens>(plain.decodeToString()).let {
            AuthTokens(it.accessToken, it.refreshToken, Instant.ofEpochMilli(it.accessExpiresAtMillis))
        }
    } catch (ignored: SerializationException) {
        null
    }

    override suspend fun save(tokens: AuthTokens) {
        val stored = StoredTokens(tokens.accessToken, tokens.refreshToken, tokens.accessExpiresAt.toEpochMilli())
        val encrypted = cipher.encrypt(json.encodeToString(stored).encodeToByteArray())
        dataStore.edit { it[sessionKey] = Base64.getEncoder().encodeToString(encrypted) }
    }

    override suspend fun clear() {
        dataStore.edit { it.remove(sessionKey) }
    }
}
