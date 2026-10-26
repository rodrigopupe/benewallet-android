package com.rpstudio.benewallet.feature.wallet.data.local

import androidx.room.withTransaction
import com.rpstudio.benewallet.feature.wallet.domain.model.BenefitBalance
import com.rpstudio.benewallet.feature.wallet.domain.model.StatementFilter
import com.rpstudio.benewallet.feature.wallet.domain.model.Transaction
import com.rpstudio.benewallet.feature.wallet.domain.model.TransactionPage
import com.rpstudio.benewallet.feature.wallet.domain.model.statementZone
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class RoomWalletLocalDataSource(private val database: WalletDatabase) : WalletLocalDataSource {
    private val transactionDao = database.transactionDao()
    private val balanceDao = database.balanceDao()
    private val syncDao = database.syncDao()

    override fun observeBalances(): Flow<List<BenefitBalance>> =
        balanceDao.observe().map { rows -> rows.mapNotNull { it.toDomainOrNull() } }

    override fun observeBalancesFetchedAt(): Flow<Instant?> =
        syncDao.observeBalanceSync().map { row -> row?.let { Instant.ofEpochMilli(it.fetchedAtMillis) } }

    override fun observeTransactions(filter: StatementFilter): Flow<List<Transaction>> = transactionDao.observe(
        category = filter.category?.name,
        fromMillis = filter.from?.atStartOfDay(statementZone)?.toInstant()?.toEpochMilli(),
        // "to" is inclusive for the user, so the SQL upper bound is the start of the next day (exclusive)
        toExclusiveMillis = filter.to?.plusDays(1)?.atStartOfDay(statementZone)?.toInstant()?.toEpochMilli()
    ).map { rows -> rows.mapNotNull { it.toDomainOrNull() } }

    override fun observeTransactionsSync(): Flow<TransactionsSync?> =
        syncDao.observeTransactionsSync().map { row -> row?.toDomain() }

    override suspend fun replaceBalances(balances: List<BenefitBalance>, fetchedAt: Instant) {
        database.withTransaction {
            balanceDao.deleteAll()
            balanceDao.upsert(balances.map { it.toEntity() })
            syncDao.upsertBalanceSync(BalanceSyncEntity(fetchedAtMillis = fetchedAt.toEpochMilli()))
        }
    }

    override suspend fun replaceTransactions(page: TransactionPage, filter: StatementFilter, fetchedAt: Instant) {
        database.withTransaction {
            transactionDao.deleteAll()
            transactionDao.upsert(page.items.map { it.toEntity() })
            syncDao.upsertTransactionsSync(
                TransactionsSyncEntity(
                    fetchedAtMillis = fetchedAt.toEpochMilli(),
                    nextCursor = page.nextCursor,
                    category = filter.category?.name,
                    fromDate = filter.from?.toString(),
                    toDate = filter.to?.toString()
                )
            )
        }
    }

    override suspend fun appendTransactions(page: TransactionPage) {
        database.withTransaction {
            transactionDao.upsert(page.items.map { it.toEntity() })
            syncDao.updateTransactionsCursor(page.nextCursor)
        }
    }

    override suspend fun clearAll() {
        database.withTransaction {
            transactionDao.deleteAll()
            balanceDao.deleteAll()
            syncDao.deleteBalanceSync()
            syncDao.deleteTransactionsSync()
        }
    }
}
