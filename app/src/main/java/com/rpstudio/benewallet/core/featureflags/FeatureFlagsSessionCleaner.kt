package com.rpstudio.benewallet.core.featureflags

import com.rpstudio.benewallet.core.auth.SessionDataCleaner

/** Flags are resolved per account, so the next account must start from the defaults, not from this one's values. */
internal class FeatureFlagsSessionCleaner(private val store: FeatureFlagStore) : SessionDataCleaner {
    override suspend fun clear() = store.replaceFlags(emptyMap())
}
