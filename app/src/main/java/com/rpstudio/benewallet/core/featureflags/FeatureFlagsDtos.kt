package com.rpstudio.benewallet.core.featureflags

import kotlinx.serialization.Serializable

@Serializable
internal data class FeatureFlagDto(val key: String, val enabled: Boolean, val rolloutPercent: Int = 0)

@Serializable
internal data class FeatureFlagsResponseDto(val flags: List<FeatureFlagDto>)
