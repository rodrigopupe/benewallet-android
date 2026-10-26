package com.rpstudio.benewallet.core.featureflags

import retrofit2.http.GET

internal interface FeatureFlagsService {
    @GET("feature-flags")
    suspend fun getFeatureFlags(): FeatureFlagsResponseDto
}
