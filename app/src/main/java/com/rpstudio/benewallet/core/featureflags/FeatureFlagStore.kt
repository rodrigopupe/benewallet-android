package com.rpstudio.benewallet.core.featureflags

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val FLAG_KEY_PREFIX = "flag."
private val installIdKey = stringPreferencesKey("install_id")

/** Keeps the repository away from DataStore, so it can be tested on the JVM with a fake. */
internal interface FeatureFlagStore {
    fun observeFlags(): Flow<Map<String, Boolean>>

    /** Replaces every cached flag: a flag removed from the BFF goes back to its default. */
    suspend fun replaceFlags(flags: Map<String, Boolean>)

    /** Random id generated once per installation. It is not personal data and is only used for rollout buckets. */
    suspend fun installId(): String
}

internal class DataStoreFeatureFlagStore(private val dataStore: DataStore<Preferences>) : FeatureFlagStore {
    override fun observeFlags(): Flow<Map<String, Boolean>> = dataStore.data.map { preferences ->
        preferences.asMap()
            .filterKeys { it.name.startsWith(FLAG_KEY_PREFIX) }
            .entries
            .associate { (key, value) -> key.name.removePrefix(FLAG_KEY_PREFIX) to (value as Boolean) }
    }

    override suspend fun replaceFlags(flags: Map<String, Boolean>) {
        dataStore.edit { preferences ->
            preferences.asMap().keys
                .filter { it.name.startsWith(FLAG_KEY_PREFIX) }
                .forEach { preferences.remove(it) }
            flags.forEach { (key, enabled) -> preferences[booleanPreferencesKey(FLAG_KEY_PREFIX + key)] = enabled }
        }
    }

    override suspend fun installId(): String {
        // Generated inside edit, which is atomic, so two concurrent first calls cannot create two ids
        val preferences = dataStore.edit { preferences ->
            if (preferences[installIdKey] == null) preferences[installIdKey] = UUID.randomUUID().toString()
        }
        return checkNotNull(preferences[installIdKey])
    }
}
