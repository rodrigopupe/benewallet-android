package com.rpstudio.benewallet.core.featureflags

import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeFeatureFlagStore(initial: Map<String, Boolean> = emptyMap()) : FeatureFlagStore {
    val flags = MutableStateFlow(initial)
    private var id: String? = null

    override fun observeFlags(): Flow<Map<String, Boolean>> = flags

    override suspend fun replaceFlags(flags: Map<String, Boolean>) {
        this.flags.value = flags
    }

    override suspend fun installId(): String = id ?: UUID.randomUUID().toString().also { id = it }
}

internal class FakeFeatureFlagsService : FeatureFlagsService {
    var result: () -> FeatureFlagsResponseDto = { error("feature flags not stubbed") }
    val userIds = mutableListOf<String?>()

    override suspend fun getFeatureFlags(userId: String?): FeatureFlagsResponseDto {
        userIds.add(userId)
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
