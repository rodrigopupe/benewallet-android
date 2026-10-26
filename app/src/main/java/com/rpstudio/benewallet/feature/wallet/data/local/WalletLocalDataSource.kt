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

/** The cached statement and its sync marker, read together: they always describe the same state of the database. */
internal data class LocalStatement(val items: List<Transaction>, val sync: TransactionsSync?)

internal interface WalletLocalDataSource {
    fun observeBalances(): Flow<List<BenefitBalance>>

    fun observeBalancesFetchedAt(): Flow<Instant?>

    /**
     * Items (for [filter]) and sync marker in a single emission. Two separate flows would let a screen see the marker
     * of a new snapshot with the items of the old one, or the other way around, for an instant after every refresh.
     */
    fun observeStatement(filter: StatementFilter): Flow<LocalStatement>

    fun observeTransactionsSync(): Flow<TransactionsSync?>

    suspend fun replaceBalances(balances: List<BenefitBalance>, fetchedAt: Instant)

    /** Atomically replaces the whole snapshot: items, cursor, filter and fetch time. */
    suspend fun replaceTransactions(page: TransactionPage, filter: StatementFilter, fetchedAt: Instant)

    /** Atomically appends an older page and moves the cursor. Does not touch the fetch time of the head. */
    suspend fun appendTransactions(page: TransactionPage)

    /** Wipes everything cached, including the sync markers: the next account must not see the previous one's data. */
    suspend fun clearAll()
}
