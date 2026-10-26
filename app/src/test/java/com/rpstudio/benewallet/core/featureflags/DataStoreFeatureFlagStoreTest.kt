package com.rpstudio.benewallet.core.featureflags

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreFeatureFlagStoreTest {
    @get:Rule
    val folder = TemporaryFolder()

    // A real DataStore on a temporary file: it runs on the JVM, so no device is needed
    private val dataStore by lazy { PreferenceDataStoreFactory.create { File(folder.root, "flags.preferences_pb") } }
    private val store by lazy { DataStoreFeatureFlagStore(dataStore) }

    @Test
    fun `starts empty`() = runTest {
        assertEquals(emptyMap<String, Boolean>(), store.observeFlags().first())
    }

    @Test
    fun `saved flags are read back`() = runTest {
        store.replaceFlags(mapOf("goals" to true, "statement_filters" to false))

        assertEquals(mapOf("goals" to true, "statement_filters" to false), store.observeFlags().first())
    }

    @Test
    fun `replacing drops the flags that are no longer served`() = runTest {
        store.replaceFlags(mapOf("goals" to true, "statement_filters" to false))

        store.replaceFlags(mapOf("goals" to false))

        assertEquals(mapOf("goals" to false), store.observeFlags().first())
    }

    @Test
    fun `other preferences in the file are not mistaken for flags`() = runTest {
        dataStore.edit { it[stringPreferencesKey("something_else")] = "x" }

        store.replaceFlags(mapOf("goals" to true))

        assertEquals(mapOf("goals" to true), store.observeFlags().first())
    }
}
