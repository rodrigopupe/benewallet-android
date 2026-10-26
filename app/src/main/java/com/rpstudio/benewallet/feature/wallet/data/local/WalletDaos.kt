package com.rpstudio.benewallet.feature.wallet.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

private const val OBSERVE_TRANSACTIONS_QUERY = "SELECT * FROM transactions " +
    "WHERE (:category IS NULL OR category = :category) " +
    "AND (:fromMillis IS NULL OR occurredAtMillis >= :fromMillis) " +
    "AND (:toExclusiveMillis IS NULL OR occurredAtMillis < :toExclusiveMillis) " +
    "ORDER BY occurredAtMillis DESC, id DESC"

@Dao
internal interface TransactionDao {
    @Query(OBSERVE_TRANSACTIONS_QUERY)
    suspend fun query(category: String?, fromMillis: Long?, toExclusiveMillis: Long?): List<TransactionEntity>

    @Upsert
    suspend fun upsert(items: List<TransactionEntity>)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}

@Dao
internal interface BalanceDao {
    @Query("SELECT * FROM balances ORDER BY benefit")
    fun observe(): Flow<List<BalanceEntity>>

    @Upsert
    suspend fun upsert(items: List<BalanceEntity>)

    @Query("DELETE FROM balances")
    suspend fun deleteAll()
}

@Dao
internal interface SyncDao {
    @Query("SELECT * FROM balance_sync WHERE id = $SYNC_SINGLETON_ID")
    fun observeBalanceSync(): Flow<BalanceSyncEntity?>

    @Upsert
    suspend fun upsertBalanceSync(entity: BalanceSyncEntity)

    @Query("SELECT * FROM transactions_sync WHERE id = $SYNC_SINGLETON_ID")
    fun observeTransactionsSync(): Flow<TransactionsSyncEntity?>

    @Query("SELECT * FROM transactions_sync WHERE id = $SYNC_SINGLETON_ID")
    suspend fun getTransactionsSync(): TransactionsSyncEntity?

    @Upsert
    suspend fun upsertTransactionsSync(entity: TransactionsSyncEntity)

    @Query("UPDATE transactions_sync SET nextCursor = :cursor WHERE id = $SYNC_SINGLETON_ID")
    suspend fun updateTransactionsCursor(cursor: String?)

    @Query("DELETE FROM balance_sync")
    suspend fun deleteBalanceSync()

    @Query("DELETE FROM transactions_sync")
    suspend fun deleteTransactionsSync()
}
