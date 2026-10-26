package com.rpstudio.benewallet.feature.wallet

import com.rpstudio.benewallet.feature.wallet.domain.model.RefreshResult
import com.rpstudio.benewallet.feature.wallet.domain.model.Statement
import com.rpstudio.benewallet.feature.wallet.domain.model.StatementFilter
import com.rpstudio.benewallet.feature.wallet.domain.model.WalletBalances
import com.rpstudio.benewallet.feature.wallet.domain.repository.WalletRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow

internal class FakeWalletRepository : WalletRepository {
    val balances = MutableStateFlow(WalletBalances(emptyList(), null))
    var refreshResult: RefreshResult = RefreshResult.Success
    val refreshCalls = mutableListOf<Boolean>()

    override fun observeBalances(): Flow<WalletBalances> = balances

    override fun observeStatement(filter: StatementFilter): Flow<Statement> = emptyFlow()

    override suspend fun refreshBalances(force: Boolean): RefreshResult {
        refreshCalls.add(force)
        return refreshResult
    }

    override suspend fun refreshStatement(filter: StatementFilter, force: Boolean) = RefreshResult.Success

    override suspend fun loadMoreStatement() = RefreshResult.Success
}
