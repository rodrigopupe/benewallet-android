package com.rpstudio.benewallet.core.featureflags

import kotlinx.coroutines.flow.Flow

internal interface FeatureFlagsRepository {
    /** Emits the cached flags (or the defaults) right away, and again after every successful refresh. */
    fun observeFlags(): Flow<FeatureFlags>

    /** Fetches the flags for this installation. A failure keeps the cache: flags never block the screen. */
    suspend fun refresh()
}
