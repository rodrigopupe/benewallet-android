package com.rpstudio.benewallet.bff

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FeatureFlagsTest {
    @Test
    fun `bucket is stable for the same flag and user`() {
        assertEquals(
            FeatureFlags.bucket("goals", "user-1"),
            FeatureFlags.bucket("goals", "user-1")
        )
    }

    @Test
    fun `0 percent disables and 100 percent enables for everyone`() {
        assertFalse(FeatureFlags.resolve("user-1", mapOf("f" to 0)).single().enabled)
        assertTrue(FeatureFlags.resolve("user-1", mapOf("f" to 100)).single().enabled)
    }

    @Test
    fun `anonymous user is only in full rollouts`() {
        assertFalse(FeatureFlags.resolve(null, mapOf("f" to 50)).single().enabled)
    }

    @Test
    fun `raising the percentage never switches a user off`() {
        val users = (1..200).map { "user-$it" }
        for (user in users) {
            val at30 = FeatureFlags.resolve(user, mapOf("f" to 30)).single().enabled
            val at60 = FeatureFlags.resolve(user, mapOf("f" to 60)).single().enabled
            assertTrue(!at30 || at60)
        }
    }
}
