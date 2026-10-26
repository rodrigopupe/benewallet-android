package com.rpstudio.benewallet.core.auth

/** Reversible and recognizable as "not plain text", without needing an Android Keystore. */
internal class FakeTokenCipher : TokenCipher {
    var failToDecrypt = false
    var encryptCalls = 0

    override fun encrypt(plain: ByteArray): ByteArray {
        encryptCalls++
        return plain.reversedArray()
    }

    override fun decrypt(encrypted: ByteArray): ByteArray? = if (failToDecrypt) null else encrypted.reversedArray()
}

internal class FakeTokenStore(var tokens: AuthTokens? = null) : TokenStore {
    override suspend fun read(): AuthTokens? = tokens

    override suspend fun save(tokens: AuthTokens) {
        this.tokens = tokens
    }

    override suspend fun clear() {
        tokens = null
    }
}
