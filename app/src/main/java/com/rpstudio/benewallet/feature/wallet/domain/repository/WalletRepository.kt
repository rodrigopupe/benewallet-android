package com.rpstudio.benewallet.feature.wallet.domain.repository

import com.rpstudio.benewallet.feature.wallet.domain.model.RefreshResult
import com.rpstudio.benewallet.feature.wallet.domain.model.Statement
import com.rpstudio.benewallet.feature.wallet.domain.model.StatementFilter
import com.rpstudio.benewallet.feature.wallet.domain.model.WalletBalances
import kotlinx.coroutines.flow.Flow

internal interface WalletRepository {
    fun observeBalances(): Flow<WalletBalances>

    fun observeStatement(filter: StatementFilter): Flow<Statement>

    /** Skips the network when the cache is fresh, unless [force] is true. */
    suspend fun refreshBalances(force: Boolean = false): RefreshResult

    /** Always hits the network when the cached snapshot belongs to another filter. */
    suspend fun refreshStatement(filter: StatementFilter, force: Boolean = false): RefreshResult

    /** Fetches the next page using the stored cursor and appends it to the snapshot. */
    suspend fun loadMoreStatement(): RefreshResult
}
