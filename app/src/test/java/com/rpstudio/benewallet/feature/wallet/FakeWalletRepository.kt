package com.rpstudio.benewallet.feature.wallet

import com.rpstudio.benewallet.feature.wallet.domain.model.RefreshResult
import com.rpstudio.benewallet.feature.wallet.domain.model.Statement
import com.rpstudio.benewallet.feature.wallet.domain.model.StatementFilter
import com.rpstudio.benewallet.feature.wallet.domain.model.WalletBalances
import com.rpstudio.benewallet.feature.wallet.domain.repository.WalletRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeWalletRepository : WalletRepository {
    val balances = MutableStateFlow(WalletBalances(emptyList(), null))
    val statement = MutableStateFlow(
        Statement(emptyList(), hasMore = false, isPartial = false, lastUpdatedAt = null)
    )
    var refreshResult: RefreshResult = RefreshResult.Success
    var loadMoreResult: RefreshResult = RefreshResult.Success
    val refreshCalls = mutableListOf<Boolean>()
    val statementRefreshCalls = mutableListOf<Pair<StatementFilter, Boolean>>()
    var loadMoreCalls = 0

    override fun observeBalances(): Flow<WalletBalances> = balances

    override fun observeStatement(filter: StatementFilter): Flow<Statement> = statement

    override suspend fun refreshBalances(force: Boolean): RefreshResult {
        refreshCalls.add(force)
        return refreshResult
    }

    override suspend fun refreshStatement(filter: StatementFilter, force: Boolean): RefreshResult {
        statementRefreshCalls.add(filter to force)
        return refreshResult
    }

    override suspend fun loadMoreStatement(): RefreshResult {
        loadMoreCalls++
        return loadMoreResult
    }
}
