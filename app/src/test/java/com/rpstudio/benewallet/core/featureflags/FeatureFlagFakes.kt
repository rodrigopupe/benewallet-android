package com.rpstudio.benewallet.core.featureflags

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeFeatureFlagStore(initial: Map<String, Boolean> = emptyMap()) : FeatureFlagStore {
    val flags = MutableStateFlow(initial)

    override fun observeFlags(): Flow<Map<String, Boolean>> = flags

    override suspend fun replaceFlags(flags: Map<String, Boolean>) {
        this.flags.value = flags
    }
}

internal class FakeFeatureFlagsService : FeatureFlagsService {
    var result: () -> FeatureFlagsResponseDto = { error("feature flags not stubbed") }
    var calls = 0

    override suspend fun getFeatureFlags(): FeatureFlagsResponseDto {
        calls++
        return result()
    }
}

internal class FakeFeatureFlagsRepository(initial: FeatureFlags = FeatureFlags()) : FeatureFlagsRepository {
    val flags = MutableStateFlow(initial)
    var refreshCalls = 0

    override fun observeFlags(): Flow<FeatureFlags> = flags

    override suspend fun refresh() {
        refreshCalls++
    }
}
