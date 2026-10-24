package com.rpstudio.benewallet.feature.wallet.data.remote

import com.rpstudio.benewallet.feature.wallet.data.remote.dto.BalanceResponseDto
import com.rpstudio.benewallet.feature.wallet.data.remote.dto.FeatureFlagsResponseDto
import com.rpstudio.benewallet.feature.wallet.data.remote.dto.TransactionsResponseDto
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

internal interface WalletService {
    @GET("balance")
    suspend fun getBalance(): BalanceResponseDto

    @GET("transactions")
    suspend fun getTransactions(
        @Query("cursor") cursor: String? = null,
        @Query("category") category: String? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("limit") limit: Int? = null
    ): TransactionsResponseDto

    @GET("feature-flags")
    suspend fun getFeatureFlags(@Header("X-User-Id") userId: String?): FeatureFlagsResponseDto
}
