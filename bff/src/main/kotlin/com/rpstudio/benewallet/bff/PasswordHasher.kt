package com.rpstudio.benewallet.bff

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

private const val ITERATIONS = 120_000
private const val KEY_BITS = 256
private const val SALT_BYTES = 16

internal class PasswordHash(val salt: ByteArray, val hash: ByteArray)

/** PBKDF2, so even the demo users are never stored (or compared) as plain text. */
internal object PasswordHasher {
    private val random = SecureRandom()

    fun hash(password: String): PasswordHash {
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        return PasswordHash(salt, derive(password, salt))
    }

    fun verify(password: String, expected: PasswordHash): Boolean =
        // Constant-time comparison: the time taken must not reveal how many bytes matched
        MessageDigest.isEqual(derive(password, expected.salt), expected.hash)

    private fun derive(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }
}
