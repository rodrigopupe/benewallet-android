package com.rpstudio.benewallet.core.featureflags

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FeatureFlagsSessionCleanerTest {
    @Test
    fun `clearing sends the next account back to the default flags`() = runTest {
        val store = FakeFeatureFlagStore(mapOf("statement_filters" to false, "goals" to true))

        FeatureFlagsSessionCleaner(store).clear()

        val flags = FeatureFlags(store.observeFlags().first())
        assertEquals("back to the default (on)", true, flags.isEnabled(FeatureFlag.STATEMENT_FILTERS))
        assertEquals("back to the default (off)", false, flags.isEnabled(FeatureFlag.GOALS))
    }
}
