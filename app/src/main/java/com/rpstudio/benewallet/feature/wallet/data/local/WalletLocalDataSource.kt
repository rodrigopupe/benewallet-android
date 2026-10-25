package com.rpstudio.benewallet.feature.wallet.data.local

import com.rpstudio.benewallet.feature.wallet.domain.model.BenefitBalance
import com.rpstudio.benewallet.feature.wallet.domain.model.StatementFilter
import com.rpstudio.benewallet.feature.wallet.domain.model.Transaction
import com.rpstudio.benewallet.feature.wallet.domain.model.TransactionPage
import java.time.Instant
import kotlinx.coroutines.flow.Flow

internal data class TransactionsSync(val fetchedAt: Instant, val nextCursor: String?, val filter: StatementFilter) {
    val hasMore: Boolean get() = nextCursor != null
}

internal interface WalletLocalDataSource {
    fun observeBalances(): Flow<List<BenefitBalance>>

    fun observeBalancesFetchedAt(): Flow<Instant?>

    fun observeTransactions(filter: StatementFilter): Flow<List<Transaction>>

    fun observeTransactionsSync(): Flow<TransactionsSync?>

    suspend fun replaceBalances(balances: List<BenefitBalance>, fetchedAt: Instant)

    /** Atomically replaces the whole snapshot: items, cursor, filter and fetch time. */
    suspend fun replaceTransactions(page: TransactionPage, filter: StatementFilter, fetchedAt: Instant)

    /** Atomically appends an older page and moves the cursor. Does not touch the fetch time of the head. */
    suspend fun appendTransactions(page: TransactionPage)
}
