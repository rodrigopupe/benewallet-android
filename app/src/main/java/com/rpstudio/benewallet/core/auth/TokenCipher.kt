package com.rpstudio.benewallet.core.auth

/** Encrypts the session tokens before they touch the disk. Behind an interface so tests do not need a Keystore. */
internal interface TokenCipher {
    fun encrypt(plain: ByteArray): ByteArray

    /** Returns null when the data cannot be decrypted (corrupted, or the key is gone): the session is just lost. */
    fun decrypt(encrypted: ByteArray): ByteArray?
}
