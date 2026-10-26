package com.rpstudio.benewallet.core.auth

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreTokenStoreTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val cipher = FakeTokenCipher()
    private val tokens = AuthTokens("access-123", "refresh-456", Instant.parse("2026-10-26T15:00:00Z"))
    private val file get() = File(folder.root, "session.preferences_pb")

    // A real DataStore on a temporary file: it runs on the JVM, so the persistence code is covered without a device
    private fun store() = DataStoreTokenStore(PreferenceDataStoreFactory.create { file }, cipher)

    @Test
    fun `saved tokens are read back`() = runTest {
        val store = store()

        store.save(tokens)

        assertEquals(tokens, store.read())
    }

    @Test
    fun `nothing is read before the first login`() = runTest {
        assertNull(store().read())
    }

    @Test
    fun `clear removes the session`() = runTest {
        val store = store()
        store.save(tokens)

        store.clear()

        assertNull(store.read())
    }

    @Test
    fun `tokens go through the cipher and never reach the disk as plain text`() = runTest {
        store().save(tokens)

        assertEquals("the cipher must be used", 1, cipher.encryptCalls)
        val onDisk = file.readBytes().decodeToString()
        assertFalse("access token found in the file", onDisk.contains(tokens.accessToken))
        assertFalse("refresh token found in the file", onDisk.contains(tokens.refreshToken))
    }

    @Test
    fun `tokens that cannot be decrypted count as no session`() = runTest {
        val store = store()
        store.save(tokens)

        cipher.failToDecrypt = true

        assertNull(store.read())
    }

    @Test
    fun `tokens that decrypt into garbage count as no session`() = runTest {
        val store = DataStoreTokenStore(
            PreferenceDataStoreFactory.create { file },
            object : TokenCipher {
                override fun encrypt(plain: ByteArray) = "not json".encodeToByteArray()

                override fun decrypt(encrypted: ByteArray) = "not json".encodeToByteArray()
            }
        )
        store.save(tokens)

        assertNull(store.read())
    }
}
