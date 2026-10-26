package com.rpstudio.benewallet.core.featureflags

import retrofit2.http.GET
import retrofit2.http.Header

internal interface FeatureFlagsService {
    @GET("feature-flags")
    suspend fun getFeatureFlags(@Header("X-User-Id") userId: String?): FeatureFlagsResponseDto
}
